package jpl.gds.db.api.sql.fetch.aggregate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchSetContainer {
    private Map<String, ProcessedBatchInfo> batchSet;
    private ProcessedBatchInfo bic;
    private List<ProcessedBatchInfo> list;

    public BatchSetContainer() {
        this.batchSet = new HashMap<>();
    }

    public ProcessedBatchInfo getBic() {
        return bic;
    }

    public void setBic(ProcessedBatchInfo bic) {
        this.bic = bic;
    }

    public Map<String, ProcessedBatchInfo> getBatchSet() {
        return batchSet;
    }

    public List<ProcessedBatchInfo> getList() {
        return list;
    }

    public void setList(List<ProcessedBatchInfo> list) {
        this.list = list;
    }

    public int size() {
        return batchSet.size();
    }

    public void add(String batchId, ProcessedBatchInfo indexCacheItem) {
        batchSet.put(batchId, indexCacheItem);
    }
}
