package tapi.api;

import io.reactivex.Single;
import okhttp3.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.utils.Numeric;
import java.net.Socket;

import java.io.*;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
@RequestMapping("/")
public class APIController
{
    private static final String ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";

    private final Map<String, ScriptData> pkMap = new ConcurrentHashMap<>();

    private final String INFURA_KEY;
    private final String INFURA_IPFS_KEY;

    private final OkHttpClient httpClient;

    private static class ScriptData
    {
        String privateKey;
        String contractAddress;
        String scriptFile;

        public ScriptData(String pk, String ca, String sf)
        {
            privateKey = pk;
            contractAddress = ca;
            scriptFile = sf;
        }
    }

    @Autowired
    public APIController()
    {
        String keys = load("../keys.secret");
        String[] sep = keys.split(",");
        INFURA_KEY = sep[0];
        INFURA_IPFS_KEY = sep[1];

        httpClient = buildClient();
    }

    /***********************************
     * Get Chain and address
     * Ask user to pick contract name and ( Token image or Metadata URI ) or default
     ***********************************/

    @GetMapping(value = "/")
    public String connect(@RequestHeader("User-Agent") String agent, Model model)
    {
        return "1_connect_user";
    }

    @GetMapping(value = "/faucet")
    public String faucet(@RequestHeader("User-Agent") String agent, Model model)
    {
        return "faucet";
    }

    @GetMapping(value = "/faucet2")
    public String faucet2(@RequestHeader("User-Agent") String agent, Model model)
    {
        return "faucet2";
    }

    /***********************************
     * receive chain, address, contract name & token image or metadataURI
     *
     * Determine next contract address
     * Create Private key for firmware & determine address (addr 1)
     * Create TokenScript by replacing token details and insert firmware address
     * Upload TokenScript to IPFS and note hash addr
     *
     * Open page to push transaction
     *
     **********************************/

    //window.location.replace('/createContract/' + userAccount + '/' + ethereum.chainId + '/' + metaData);
    @GetMapping(value = "/createContract/{account}/{chainId}/{metadata}/{tokenName}/{tokenSymbol}")
    String handleTokenScriptGen(@PathVariable("account") String account,
                       @PathVariable("chainId") String chainIdStr,
                       @PathVariable("metadata") String metadata,
                       @PathVariable("tokenName") String tokenName,
                       @PathVariable("tokenSymbol") String tokenSymbol,
                       Model model) {

        try
        {
            tokenName = URLDecoder.decode(tokenName, StandardCharsets.UTF_8.toString());
            byte[] metadataBytes = Base64.getUrlDecoder().decode(metadata); //URLDecoder.decode(metadata, StandardCharsets.UTF_8.toString());
            tokenSymbol = URLDecoder.decode(tokenSymbol, StandardCharsets.UTF_8.toString());

            metadata = new String(metadataBytes, UTF_8);

            //1. create web3j for this node
            long chainId = parseChainId(chainIdStr);

            Web3j web3j = EthereumNode.createWeb3jNode(chainId, INFURA_KEY);

            if (web3j == null) {
                model.addAttribute("message", "Sorry, chain: " + chainId + " Not supported.");
                return "show_error";
            }

            //2. determine next contract address

            BigInteger constructorNonce = getLastTransactionNonce(web3j, account).blockingGet();

            String nextContract = calculateContractAddress(account, constructorNonce.longValue());

            //Create privatekey for the firmware, and to complete the TokenScript
            ECKeyPair ecKeyPair = Keys.createEcKeyPair();
            BigInteger publicKey = ecKeyPair.getPublicKey();
            String iotAddr = "0x" + Keys.getAddress(publicKey);

            //3. Create the TokenScript file
            String scriptData = createTokenScriptData(iotAddr, chainId, nextContract);

            File tsFile = storeTokenScript(scriptData);

            pkMap.put(account.toLowerCase(), new ScriptData(ecKeyPair.getPrivateKey().toString(16), nextContract, tsFile.getAbsolutePath()));

            //attempt to get the IPFS hash
            String ipfsPath = "ipfs://" + uploadIPFS(tsFile, false);

            if (metadata.equals("default"))
            {
                metadata = "ipfs://QmZfmPu4riw9BqmUe8SejhJRKw5xRu78JhvYEDuYqBqeFu"; //default metadata
            }

            //load contract data
            String contractData = loadFile("contract5169.bin");

            String encodedConstructor = FunctionEncoder
                    .encodeConstructor(Arrays.<Type>asList( new org.web3j.abi.datatypes.Utf8String(ipfsPath),
                            new org.web3j.abi.datatypes.Utf8String(metadata),
                            new org.web3j.abi.datatypes.Utf8String(tokenName),
                            new org.web3j.abi.datatypes.Utf8String(tokenSymbol)));

            String deploymentData = contractData + encodedConstructor;

            // create contract
            model.addAttribute("account", "'" + account + "'");
            model.addAttribute("contract_data", "0x" + deploymentData);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return "2_mint_contract";
    }

    private long parseChainId(String chainIdStr)
    {
        if (chainIdStr.startsWith("0x"))
        {
            return Numeric.toBigInt(chainIdStr).longValue();
        }
        else
        {
            return Long.parseLong(chainIdStr);
        }
    }

    private String createTokenScriptData(String iotAddr, long chainId, String nextContract)
    {
        String scriptData = loadFile("script_template.xml");

        //now replace tokens
        scriptData = scriptData.replace("[TOKEN_NETWORK]", Long.toString(chainId));
        scriptData = scriptData.replace("[TOKEN_ADDRESS]", nextContract);
        scriptData = scriptData.replace("[IOT_ADDR]", iotAddr);
        scriptData = scriptData.replace("[IOT_ADDR]", iotAddr);

        return scriptData;
    }


    @GetMapping(value = "/displaytxhash/{account}/{txhash}/{chainId}")
    String handleTxHash(@PathVariable("account") String account,
                                @PathVariable("txhash") String txHash,
                                @PathVariable("chainId") String chainIdStr,
                                Model model) {

        ScriptData scriptData = pkMap.get(account.toLowerCase());
        if (scriptData == null) return "script_error";

        File tokenScript = new File(scriptData.scriptFile);

        if (!tokenScript.exists()) return "script_error";

        String hash = uploadIPFS(tokenScript, true);
        //now delete the file
        tokenScript.delete();

        model.addAttribute("tx_hash", txHash);
        model.addAttribute("account", "'" + account + "'");
        model.addAttribute("chainId", "'" + chainIdStr + "'");

        return "3_show_tx_complete";
    }

    /*********************************************
     * Create the firmware using the PK and contract address from last step
     ************************************/

    @GetMapping(value = "/createFirmware/{account}/{data}/{chainId}")
    String createFirmware(@PathVariable("account") String account,
                        @PathVariable("data") String data,
                        @PathVariable("chainId") String chainIdStr,
                        Model model) {

        try
        {
            String decoded = new String(java.util.Base64.getDecoder().decode(data), UTF_8);
            String[] parse = decoded.split(" ");
            long chainId = parseChainId(chainIdStr);

            for (int i = 0; i < parse.length; i++) {
                parse[i] = URLDecoder.decode(parse[i], StandardCharsets.UTF_8.toString());
            }

            ScriptData scriptData = pkMap.get(account.toLowerCase());
            if (scriptData == null) return "script_error";

            //load the firmware
            String code = loadFile("firmware.cpp");

            //replace the following:
            //[SSID] [PASSWORD] [LOCK_CONTRACT] [DEVICE_KEY] [AUGUST_LOCK_CREDENTIALS] [CHAIN_NAME]

            code = code.replace("[SSID]", parse[0]);
            code = code.replace("[PASSWORD]", parse[1]);
            code = code.replace("[LOCK_CONTRACT]", scriptData.contractAddress);// scriptData.contractAddress);
            code = code.replace("[DEVICE_KEY]", scriptData.privateKey);//scriptData.privateKey);
            code = code.replace("[AUGUST_LOCK_CREDENTIALS]", parse[2]);
            code = code.replace("[CHAIN_ID]", String.valueOf(chainId));

            //now remove the map entry
            pkMap.remove(account.toLowerCase());

            //now display the code
            //base64 encode the code
            String encoded = new String(java.util.Base64.getEncoder().encode(code.getBytes(UTF_8)), UTF_8);

            model.addAttribute("code", encoded);

            return "4_show_firmware_code";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "script_error";
        }
    }

    @GetMapping(value = "/errorFinal/{message}")
    String errorFinal(@PathVariable("message") String message,
                          Model model) {

        try
        {
            model.addAttribute("message", message);
            return "show_error";
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "script_error";
        }
    }

    private String calculateContractAddress(String account, long nonce)
    {
        byte[] addressAsBytes = Numeric.hexStringToByteArray(account);
        byte[] calculatedAddressAsBytes =
                Hash.sha3(RlpEncoder.encode(
                        new RlpList(
                                RlpString.create(addressAsBytes),
                                RlpString.create((nonce)))));

        calculatedAddressAsBytes = Arrays.copyOfRange(calculatedAddressAsBytes,
                12, calculatedAddressAsBytes.length);
        return Keys.toChecksumAddress(Numeric.toHexString(calculatedAddressAsBytes));
    }

    public Single<BigInteger> getLastTransactionNonce(Web3j web3j, String walletAddress)
    {
        return Single.fromCallable(() -> {
            try
            {
                EthGetTransactionCount ethGetTransactionCount = web3j
                        .ethGetTransactionCount(walletAddress, DefaultBlockParameterName.PENDING)
                        .send();
                return ethGetTransactionCount.getTransactionCount();
            }
            catch (Exception e)
            {
                return BigInteger.ZERO;
            }
        });
    }

    private File storeTokenScript(String tokendata)
    {
        File file = null;
        try {
            UUID uuid = UUID.randomUUID();
            File filePath = new File("../" + uuid.toString());

            FileOutputStream fos = new FileOutputStream(filePath);
            OutputStream os = new BufferedOutputStream(fos);
            os.write(tokendata.getBytes());
            fos.flush();
            os.close();
            fos.close();

            file = filePath;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return file;
    }


    /***********************************
     * File handling
     ***********************************/

    private String load(String fileName) {
        String rtn = "";
        try {
            char[] array = new char[2048];
            FileReader r = new FileReader(fileName);
            r.read(array);

            rtn = new String(array);
            r.close();

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return rtn;
    }

    private String loadFile(String fileName) {
        byte[] buffer = new byte[0];
        try {
            InputStream in = getClass()
                    .getClassLoader().getResourceAsStream(fileName);
            buffer = new byte[in.available()];
            int len = in.read(buffer);
            if (len < 1) {
                throw new IOException("Nothing is read.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new String(buffer);
    }

    private static final MediaType MEDIA_TYPE_TOKENSCRIPT
            = MediaType.parse("text/xml; charset=UTF-8");

    private OkHttpClient buildClient()
    {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    private BigInteger getGasPrice(Web3j web3j)
    {
        BigInteger gasPrice = BigInteger.valueOf(2000000000L);
        try {
            gasPrice = web3j.ethGasPrice().send().getGasPrice();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return gasPrice;
    }

    //Comms
    private String uploadIPFS(File tsFile, boolean doUpload)
    {
        String hash = "";
        String command = doUpload ? "" : "?only-hash=true";
        String APIHeader = "Basic " + INFURA_IPFS_KEY;
        try
        {
            RequestBody fileBody = RequestBody.Companion.create(tsFile, MEDIA_TYPE_TOKENSCRIPT);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "tokenscript", fileBody)
                    .build();

            Request request = new Request.Builder().url("https://ipfs.infura.io:5001/api/v0/add" + command)
                    .post(requestBody)
                    .addHeader("Authorization", APIHeader)
                    .build();

            okhttp3.Response response = httpClient.newCall(request).execute();

            String result = response.body().string();

            JSONObject data = new JSONObject(result);

            hash = data.getString("Hash");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return hash;
    }
}