package at.jit.camundaelasticspike;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProcessEndListener implements JavaDelegate {
    @Value("${elastic-search.id}")
    private String id;

    @Value("${elastic-search.key}")
    private String key;

    @Value("${elastic-search.cloudId}")
    private String cloudId;

    @Value("${elastic-search.index-name}")
    private String indexName;

    @Override
    public void execute(final DelegateExecution delEx) throws Exception {
        final String processInstanceId = delEx.getProcessInstanceId();
        deleteFromElasticSearch(processInstanceId);
    }

    void deleteFromElasticSearch(final String processInstanceId) {
        RestHighLevelClient client = null;
        try {
            final RestClientBuilder clientBuilder = RestClient.builder(cloudId);
            client = new RestHighLevelClient(
                    clientBuilder
            );
            final RequestOptions.Builder optionsBuilder = RequestOptions.DEFAULT.toBuilder();
            optionsBuilder.addHeader("Authorization", String.format("ApiKey %s",
                    ElasticSearchUtils.composeBase64ApiKey(id, key)));
            final RequestOptions requestOptions = optionsBuilder.build();

            final DeleteRequest request = new DeleteRequest(indexName,
                    ElasticSearchUtils.composeElasticId(processInstanceId));
            final DeleteResponse response = client.delete(request, requestOptions);
            if ("NOT_FOUND".equalsIgnoreCase(response.getResult().name())) {
                throw new IllegalArgumentException("NOT_FOUND");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(client);
        }
    }
}
