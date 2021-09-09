package pers.chris.dbSync.job;

import pers.chris.dbSync.conf.DBConf;
import pers.chris.dbSync.conf.SyncDataConf;
import pers.chris.dbSync.reader.Reader;
import pers.chris.dbSync.syncData.SyncData;
import pers.chris.dbSync.util.LoadClassUtil;
import pers.chris.dbSync.writer.Writer;
import org.apache.log4j.Logger;

public class DBSyncJob implements Runnable {

    private String jobId;
    private SyncDataConf syncDataConf;
    private DBConf writerConf;
    private DBConf readerConf;
    private SyncData syncData;
    private Writer writer;
    private Reader reader;
    private final Logger logger = Logger.getLogger(DBSyncJob.class);

    @Override
    public void run() {
        syncData = new SyncData();
        try {
            writer = LoadClassUtil.loadWriterClass(writerConf.getDBType());
            reader = LoadClassUtil.loadReaderClass(readerConf.getDBType());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        syncData.setSyncDataConfig(syncDataConf);
        writer.setWriterConfig(writerConf);
        reader.setReaderConfig(readerConf);

        syncData.registerListener(event -> writer.write(syncData));
        writer.connect();
        reader.connect();

        reader.read(syncData, syncDataConf.getInterval());
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public DBConf getWriterConf() {
        return writerConf;
    }

    public void setWriterConf(DBConf writerConf) {
        this.writerConf = writerConf;
    }

    public DBConf getReaderConf() {
        return readerConf;
    }

    public void setReaderConf(DBConf readerConf) {
        this.readerConf = readerConf;
    }

    public SyncDataConf getSyncDataConf() {
        return syncDataConf;
    }

    public void setSyncDataConf(SyncDataConf syncDataConf) {
        this.syncDataConf = syncDataConf;
    }
}
