package at.jit.camundaelasticspike;

import org.apache.commons.io.IOUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static at.jit.camundaelasticspike.ElasticSearchUtils.composeElasticId;

@Component
public class ProcessStartListener implements ExecutionListener {
    @Value("${elastic-search.id}")
    private String id;

    @Value("${elastic-search.key}")
    private String key;

    @Value("${elastic-search.cloudId}")
    private String cloudId;

    @Value("${elastic-search.index-name}")
    private String indexName;

    @Override
    public void notify(final DelegateExecution delEx) throws Exception {
        final String businessKey = delEx.getBusinessKey();
        final String processDefId = delEx.getProcessDefinitionId();
        final String processInstanceId = delEx.getProcessInstanceId();
        final Map<String, Object> data = new HashMap<>();
        data.put("postDate", Long.toString(new Date().getTime()));
        data.put("businessKey", businessKey);
        data.put("processDefId", processDefId);
        data.put("processInstanceId", processInstanceId);
        data.put("system", "Camunda");
        for (Map.Entry<String, Object> curVariable : delEx.getVariables().entrySet()) {
            final String variableName = curVariable.getKey();
            final Object variableValue = curVariable.getValue();
            data.put(String.format("Variable-%s", variableName), variableValue);
        }
        sendStuffToElasticSearch(data);
    }
    void sendStuffToElasticSearch(final Map<String, Object> data) {
        RestHighLevelClient client = null;
        try {
            final RestClientBuilder clientBuilder = RestClient.builder(cloudId);
            client = new RestHighLevelClient(
                    clientBuilder
            );
            RequestOptions.Builder optionsBuilder = RequestOptions.DEFAULT.toBuilder();
            optionsBuilder.addHeader("Authorization", String.format("ApiKey %s",
                    ElasticSearchUtils.composeBase64ApiKey(id, key)));
            final RequestOptions requestOptions = optionsBuilder.build();

            IndexRequest request = new IndexRequest(indexName);
            request.id(composeElasticId((String) data.get("processInstanceId")));
            request.source(data, XContentType.JSON);
            client.index(request, requestOptions);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(client);
        }
    }


}
