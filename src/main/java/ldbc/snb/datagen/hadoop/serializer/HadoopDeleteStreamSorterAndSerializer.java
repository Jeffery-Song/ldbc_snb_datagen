package ldbc.snb.datagen.hadoop.serializer;

import ldbc.snb.datagen.hadoop.DatagenHadoopJob;
import ldbc.snb.datagen.hadoop.HadoopConfiguration;
import ldbc.snb.datagen.hadoop.generator.HadoopDeleteEventKeyPartitioner;
import ldbc.snb.datagen.hadoop.key.updatekey.DeleteEventKey;
import ldbc.snb.datagen.hadoop.key.updatekey.DeleteEventKeyGroupComparator;
import ldbc.snb.datagen.util.LdbcConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class HadoopDeleteStreamSorterAndSerializer extends DatagenHadoopJob {

    public HadoopDeleteStreamSorterAndSerializer(LdbcConfiguration conf, Configuration hadoopConf) {
        super(conf, hadoopConf);
    }

    public static class HadoopDeleteStreamSorterAndSerializerReducer extends StreamSorterAndSerializerReducer<DeleteEventKey, Text, DeleteEventKey, Text> {
        @Override
        public void reduce(DeleteEventKey key, Iterable<Text> valueSet, Context context) {
            OutputStream out;
            try {
                FileSystem fs = FileSystem.get(hadoopConf);
                if (compressed) {
                    Path outFile = new Path(
                            conf.getSocialNetworkDir() + "/deleteStream_" + key.reducerId + "_" + key.partition + "_" + streamType + ".csv.gz");
                    out = new GZIPOutputStream(fs.create(outFile));
                } else {
                    Path outFile = new Path(conf.getSocialNetworkDir() + "/deleteStream_" + key.reducerId + "_" + key.partition + "_" + streamType + ".csv");
                    out = fs.create(outFile);
                }
                for (Text t : valueSet) {
                    out.write(t.toString().getBytes(StandardCharsets.UTF_8));
                }
                out.close();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public void run(List<String> inputFileNames, String type) throws Exception {

        int numThreads = HadoopConfiguration.getNumThreads(hadoopConf);
        hadoopConf.set("streamType", type);

        Job job = Job.getInstance(hadoopConf, "Delete Stream Serializer");
        job.setMapOutputKeyClass(DeleteEventKey.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(DeleteEventKey.class);
        job.setOutputValueClass(Text.class);
        job.setJarByClass(HadoopDeleteStreamSorterAndSerializerReducer.class);
        job.setReducerClass(HadoopDeleteStreamSorterAndSerializerReducer.class);
        job.setNumReduceTasks(numThreads);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        job.setPartitionerClass(HadoopDeleteEventKeyPartitioner.class);
        job.setGroupingComparatorClass(DeleteEventKeyGroupComparator.class);
        //job.setSortComparatorClass(UpdateEventKeySortComparator.class);

        for (String s : inputFileNames) {
            FileInputFormat.addInputPath(job, new Path(s));
        }
        FileOutputFormat.setOutputPath(job, new Path(conf.getBuildDir() + "/aux"));
        if (!job.waitForCompletion(true)) {
            throw new Exception();
        }

        try {
            FileSystem fs = FileSystem.get(hadoopConf);
            fs.delete(new Path(conf.getBuildDir() + "/aux"), true);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
