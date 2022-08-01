package eu.csgroup.coprs.monitoring.traceingestor;

import eu.csgroup.coprs.monitoring.common.ingestor.EntityIngestor;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/*@RunWith(SpringRunner.class)
@ContextConfiguration(classes = LogApplication.class)
@SpringBootTest*/
public class LogSinkApplicationTests {

    @Autowired
    private EntityIngestor traceService;

    @Test
    public void testContextLoads() {
    }
    
    /*private static final String ASSET_IDX = "assets";

    private static final String AUTH_URL = "https://reprocessing-auxiliary.copernicus.eu/auth/realms/reprocessing-preparation/protocol/openid-connect/token";

    @ClassRule
    public static ElasticsearchContainer elasticsearch = CommonContainers.elasticsearchContainer();

    AssetCatalogClientManager clientManager = ElasticsearchAssetCatalogClientManager.withDefaultCodec();
    Catalog catalog = ElasticsearchCatalog.builder()
        .hostPorts(hostDbPorts(elasticsearch))
        .useSSL(false)
        .credentials(Option.none())
        .readTimeout(Duration.ofSeconds(2))
        .build();

    @Ignore("Kong response is null") // FIXME: ?
    @Test
    public void testWithAuxip() throws Exception{
        AuxipTestProperties auxipTestProperties = AuxipTestProperties.FROM_ENV;

        AuxipSourceProperties properties = new AuxipSourceProperties();
        properties.setAuthServerUrl(AUTH_URL);
        PasswordGrant passwordGrant = new PasswordGrant(
            "reprocessing-preparation",
            auxipTestProperties.getUsername(),
            auxipTestProperties.getPassword(),
            null
        );
        properties.setPasswordGrant(passwordGrant);
        String baseUrl = auxipTestProperties.getBaseUrl();
        properties.setServiceUrl(baseUrl);
        properties.setCatalogIndex(ASSET_IDX);

        AuxipQueryBuilder queryBuilder = new AuxipQueryBuilderImpl(
            Vector.of(S2A),
            Try.of(() -> (Seq<AuxType>)S2AuxList.loadTypes("s2_aux_types.test.json")).get()
        );

        OAuth2Endpoint endpoint = OAuth2Endpoint.builder(AUTH_URL, passwordGrant).build();
        OAuth2HttpClient httpClient = OAuth2HttpClient.builder(AUTH_URL, endpoint)
            .setSkipSSLVerification(true)
            .setProxy(null)
            .setObjectMapper(new KongJacksonObjectMapper())
            .setAcceptedContentype(APPLICATION_JSON_VALUE)
            .build();

        AuxipPoller poller = new AuxipPoller(properties, httpClient, queryBuilder);

        DownloadAuxProperties downloadProperties = new DownloadAuxProperties();
        downloadProperties.setAuthServerUrl(AUTH_URL);
        downloadProperties.setCatalogIndex(ASSET_IDX);
        downloadProperties.setPasswordGrant(passwordGrant);
        downloadProperties.setServiceUrl(baseUrl);
        downloadProperties.setProductDownloadTimeout(Duration.ofMinutes(5));
        downloadProperties.setBaseRepository(Files.createTempDirectory("DownloadAuxApplicationTests-testWithAuxip").toFile().getAbsolutePath());

        DownloadAuxSink download = new DownloadAuxSink(downloadProperties, httpClient, catalog, clientManager);
        Thread.sleep(20000);
        var auxProducts = poller.get().collectList().block();
        for(var product: auxProducts){
            download.accept(product);
        }
    }

    @Test
    @Ignore("Can't connect to prip outside of IVV/prod environments")
    // FIXME: need to add a mock server
    public void testWithPrip() throws Exception{
        var props = PripTestProperties.FROM_ENV;
        String baseUrl = props.getBaseUrl();
        PasswordGrant passwordGrant = new PasswordGrant(null, props.getUsername(), props.getPassword(), null);

        OAuth2Endpoint endpoint = OAuth2Endpoint.builder(baseUrl, passwordGrant).build();
        OAuth2HttpClient httpClient = OAuth2HttpClient.builder(baseUrl, endpoint)
            .setSkipSSLVerification(true)
            .setProxy(null)
            .setObjectMapper(new KongJacksonObjectMapper())
            .setAcceptedContentype(APPLICATION_JSON_VALUE)
            .build();

        AuxipSourceProperties properties = new AuxipSourceProperties();
        properties.setAuthServerUrl(null);
        properties.setPasswordGrant(passwordGrant);
        properties.setServiceUrl(baseUrl);
        properties.setCatalogIndex(ASSET_IDX);
        properties.setSkipServiceSslVerification(true);


        AuxipQueryBuilder queryBuilder = new AuxipQueryBuilderImpl(
            Vector.of(S2A),
            Try.of(() -> (Seq<AuxType>)S2AuxList.loadTypes("s2_aux_types.test.json")).get()
        );

        AuxipPoller poller = new AuxipPoller(properties, httpClient, queryBuilder);
        Thread.sleep(20000);
        //var auxProducts = poller.poll().get();

        DownloadAuxProperties downloadProperties = new DownloadAuxProperties();
        downloadProperties.setAuthServerUrl(null);
        downloadProperties.setPasswordGrant(passwordGrant);
        downloadProperties.setServiceUrl(baseUrl);
        downloadProperties.setCatalogIndex(ASSET_IDX);
        downloadProperties.setProductDownloadTimeout(Duration.ofMinutes(5));
        downloadProperties.setBaseRepository("/shared/sdorgan/aux");
        downloadProperties.setSkipServiceSslVerification(true);

        DownloadAuxSink download = new DownloadAuxSink(downloadProperties, httpClient, catalog, clientManager);
        Thread.sleep(20000);
        var auxProducts = poller.get().collectList().block();
        for(var product: auxProducts){
            download.accept(product);
        }
    }

    @Test
    public void testSer() throws Exception{
        String resourceName = "test.json";
        InputStream inStream = DownloadAuxApplicationTests.class.getClassLoader().getResourceAsStream(resourceName);
        String content = new String(inStream.readAllBytes());
        System.out.println(content);
        var mapper = new KongJacksonObjectMapper();
        mapper.readValue(content, ProductList.class);
    }
*/
}
