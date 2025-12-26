package com.bovinemagnet.pgconsole.model;

public class TableStats {
    private String schemaName;
    private String tableName;
    private long seqScan;
    private long seqTupRead;
    private long idxScan;
    private long idxTupFetch;
    private long nTupIns;
    private long nTupUpd;
    private long nTupDel;
    private long nLiveTup;
    private long nDeadTup;
    
    public TableStats() {}
    
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public long getSeqScan() { return seqScan; }
    public void setSeqScan(long seqScan) { this.seqScan = seqScan; }
    public long getSeqTupRead() { return seqTupRead; }
    public void setSeqTupRead(long seqTupRead) { this.seqTupRead = seqTupRead; }
    public long getIdxScan() { return idxScan; }
    public void setIdxScan(long idxScan) { this.idxScan = idxScan; }
    public long getIdxTupFetch() { return idxTupFetch; }
    public void setIdxTupFetch(long idxTupFetch) { this.idxTupFetch = idxTupFetch; }
    public long getnTupIns() { return nTupIns; }
    public void setnTupIns(long nTupIns) { this.nTupIns = nTupIns; }
    public long getnTupUpd() { return nTupUpd; }
    public void setnTupUpd(long nTupUpd) { this.nTupUpd = nTupUpd; }
    public long getnTupDel() { return nTupDel; }
    public void setnTupDel(long nTupDel) { this.nTupDel = nTupDel; }
    public long getnLiveTup() { return nLiveTup; }
    public void setnLiveTup(long nLiveTup) { this.nLiveTup = nLiveTup; }
    public long getnDeadTup() { return nDeadTup; }
    public void setnDeadTup(long nDeadTup) { this.nDeadTup = nDeadTup; }
    
    public double getBloatRatio() {
        if (nLiveTup == 0) return 0.0;
        return (double) nDeadTup / (double) (nLiveTup + nDeadTup) * 100.0;
    }
}
