package tapi.api;

import okhttp3.OkHttpClient;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.TimeUnit;

public class EthereumNode
{
    static final int MAINNET_ID = 1;
    static final int CLASSIC_ID = 61;
    static final int POA_ID = 99;
    static final int KOVAN_ID = 42;
    static final int ROPSTEN_ID = 3;
    static final int SOKOL_ID = 77;
    static final int RINKEBY_ID = 4;
    static final int XDAI_ID = 100;
    static final int GOERLI_ID = 5;
    static final int ARTIS_SIGMA1_ID = 246529;
    static final int ARTIS_TAU1_ID = 246785;
    static final int BINANCE_TEST_ID = 97;
    static final int BINANCE_MAIN_ID = 56;
    static final int HECO_ID = 128;
    static final int HECO_TEST_ID = 256;
    static final int FANTOM_ID = 250;
    static final int FANTOM_TEST_ID = 4002;
    static final int AVALANCHE_ID = 43114;
    static final int FUJI_TEST_ID = 43113;
    static final int POLYGON_ID = 137;
    static final int MUMBAI_TEST_ID = 80001;
    static final int OPTIMISTIC_MAIN_ID = 10;
    static final int OPTIMISTIC_TEST_ID = 69;
    static final int CRONOS_TEST_ID = 338;
    static final int ARBITRUM_MAIN_ID = 42161;
    static final int ARBITRUM_TEST_ID = 421611;
    //static final int PALM_ID = 11297108109L;
    //static final int PALM_TEST_ID = 11297108099L;
    static final int KLAYTN_ID = 8217;
    static final int KLAYTN_BOABAB_ID = 1001;
    static final int IOTEX_MAINNET_ID = 4689;
    static final int IOTEX_TESTNET_ID = 4690;
    static final int AURORA_MAINNET_ID = 1313161554;
    static final int AURORA_TESTNET_ID = 1313161555;
    static final int MILKOMEDA_C1_ID = 2001;
    static final int MILKOMEDA_C1_TEST_ID = 200101;

    static String MAINNET_RPC_URL = "https://mainnet.infura.io/v3/";
    static String RINKEBY_RPC_URL = "https://rinkeby.infura.io/v3/";
    static String KOVAN_RPC_URL = "https://kovan.infura.io/v3/";
    static String GOERLI_RPC_URL = "https://goerli.infura.io/v3/";
    static String POLYGON_RPC_URL = "https://polygon-mainnet.infura.io/v3/";
    static String ARBITRUM_RPC_URL = "https://arbitrum-mainnet.infura.io/v3/";
    static String MUMBAI_RPC_URL = "https://polygon-mumbai.infura.io/v3/";
    static String OPTIMISM_RPC_URL = "https://optimism-mainnet.infura.io/v3/";
    static String OPTIMISM_TESTRPC_URL = "https://optimism-kovan.infura.io/v3/";
    static String ARBITRUM_TEST_RPC_URL = "https://arbitrum-rinkeby.infura.io/v3/";
    static String PALM_RPC_URL = "https://palm-mainnet.infura.io/v3/";
    static String PALM_TEST_RPC_URL = "https://palm-testnet.infura.io/v3/";

    static String CLASSIC_RPC_URL = "https://www.ethercluster.com/etc";
    static String XDAI_RPC_URL = "https://rpc.xdaichain.com";
    static String POA_RPC_URL = "https://core.poa.network";
    static String SOKOL_RPC_URL = "https://sokol.poa.network";
    static String ARTIS_SIGMA1_RPC_URL = "https://rpc.sigma1.artis.network";
    static String ARTIS_TAU1_RPC_URL = "https://rpc.tau1.artis.network";
    static String BINANCE_TEST_RPC_URL = "https://data-seed-prebsc-1-s3.binance.org:8545";
    static String BINANCE_MAIN_RPC_URL = "https://bsc-dataseed.binance.org";
    static String HECO_RPC_URL = "https://http-mainnet.hecochain.com";
    static String HECO_TEST_RPC_URL = "https://http-testnet.hecochain.com";
    static String CRONOS_TEST_URL = "https://cronos-testnet.crypto.org:8545";
    static String IOTEX_MAINNET_RPC_URL = "https://babel-api.mainnet.iotex.io";
    static String IOTEX_TESTNET_RPC_URL = "https://babel-api.testnet.iotex.io";
    static String AURORA_MAINNET_RPC_URL = "https://mainnet.aurora.dev";
    static String AURORA_TESTNET_RPC_URL = "https://testnet.aurora.dev";
    static String MILKOMEDA_C1_RPC = "https://rpc-mainnet-cardano-evm.c1.milkomeda.com";
    static String MILKOMEDA_C1_TEST_RPC = "https://rpc-devnet-cardano-evm.c1.milkomeda.com";
    static String KLAYTN_RPC = "https://public-node-api.klaytnapi.com/v1/cypress";
    static String KLAYTN_BAOBAB_RPC = "https://api.baobab.klaytn.net:8651";

    private static OkHttpClient buildClient()
    {
        return new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    public static Web3j createWeb3jNode(long chainId, String infuraKey)
    {
        String node;
        int chainIdT = (int)chainId;
        switch (chainIdT)
        {
            case MAINNET_ID:
                node = MAINNET_RPC_URL;
                break;
            case CLASSIC_ID:
                node = CLASSIC_RPC_URL;
                break;
            case POA_ID:
                node = POA_RPC_URL;
                break;
            case KOVAN_ID:
                node = KOVAN_RPC_URL;
                break;
            case SOKOL_ID:
                node = SOKOL_RPC_URL;
                break;
            case RINKEBY_ID:
                node = RINKEBY_RPC_URL;
                break;
            case XDAI_ID:
                node = XDAI_RPC_URL;
                break;
            case GOERLI_ID:
                node = GOERLI_RPC_URL;
                break;
            case KLAYTN_ID:
                node = KLAYTN_RPC;
                break;
            case KLAYTN_BOABAB_ID:
                node = KLAYTN_BAOBAB_RPC;
                break;
            case IOTEX_MAINNET_ID:
                node = IOTEX_MAINNET_RPC_URL;
                break;
            case IOTEX_TESTNET_ID:
                node = IOTEX_TESTNET_RPC_URL;
                break;
            case POLYGON_ID:
                node = POLYGON_RPC_URL;
                break;
            case MUMBAI_TEST_ID:
                node = MUMBAI_RPC_URL;
                break;
            case MILKOMEDA_C1_ID:
                node = MILKOMEDA_C1_RPC;
                break;
            case MILKOMEDA_C1_TEST_ID:
                node = MILKOMEDA_C1_TEST_RPC;
                break;
            case BINANCE_MAIN_ID:
                node = BINANCE_MAIN_RPC_URL;
                break;
            case BINANCE_TEST_ID:
                node = BINANCE_TEST_RPC_URL;
                break;
            case OPTIMISTIC_MAIN_ID:
                node = OPTIMISM_RPC_URL;
                break;
            case OPTIMISTIC_TEST_ID:
                node = OPTIMISM_TESTRPC_URL;
                break;
            case ARBITRUM_MAIN_ID:
                node = ARBITRUM_RPC_URL;
                break;
            case ARBITRUM_TEST_ID:
                node = ARBITRUM_TEST_RPC_URL;
                break;

            default:
                node = "";
                return null;
        }

        //create web3j node
        if (node.contains("infura"))
        {
            node = node + infuraKey;
        }

        HttpService nodeService = new HttpService(node,  buildClient(), false);
        return Web3j.build(nodeService);
    }

}
