/* 
 Copyright (c) 2013 LDBC
 Linked Data Benchmark Council (http://www.ldbcouncil.org)
 
 This file is part of ldbc_snb_datagen.
 
 ldbc_snb_datagen is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 ldbc_snb_datagen is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with ldbc_snb_datagen.  If not, see <http://www.gnu.org/licenses/>.
 
 Copyright (C) 2011 OpenLink Software <bdsmt@openlinksw.com>
 All Rights Reserved.
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation;  only Version 2 of the License dated
 June 1991.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.*/
package ldbc.snb.datagen.hadoop.generator;

import ldbc.snb.datagen.DatagenContext;
import ldbc.snb.datagen.DatagenParams;
import ldbc.snb.datagen.entities.dynamic.person.Person;
import ldbc.snb.datagen.generator.generators.PersonGenerator;
import ldbc.snb.datagen.hadoop.DatagenHadoopJob;
import ldbc.snb.datagen.hadoop.HadoopConfiguration;
import ldbc.snb.datagen.hadoop.key.TupleKey;
import ldbc.snb.datagen.hadoop.miscjob.keychanger.HadoopFileKeyChanger;
import ldbc.snb.datagen.util.LdbcConfiguration;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class HadoopPersonGenerator extends DatagenHadoopJob {

    public HadoopPersonGenerator(LdbcConfiguration conf, Configuration hadoopConf) {
        super(conf, hadoopConf);
    }

    public static class HadoopPersonGeneratorMapper extends Mapper<LongWritable, Text, TupleKey, Person> {

        private HadoopFileKeyChanger.KeySetter<TupleKey> keySetter = null;

        @Override
        public void map(LongWritable key, Text value, Context context) {

            Configuration hadoopConf = context.getConfiguration();
            LdbcConfiguration conf = HadoopConfiguration.extractLdbcConfig(hadoopConf);

            try {
                this.keySetter = (HadoopFileKeyChanger.KeySetter) Class.forName(hadoopConf.get("postKeySetterName")).newInstance();
            } catch (Exception e) {
                System.err.println("Error when setting key setter");
                System.err.println(e.getMessage());
                throw new RuntimeException(e);
            }

            int threadId = Integer.parseInt(value.toString());
            System.out.println("Generating person at mapper " + threadId);
            DatagenContext.initialize(conf);

            // Here we determine the blocks in the "block space" that this mapper is responsible for.
            int numBlocks = (int) (Math.ceil(DatagenParams.numPersons / (double) DatagenParams.blockSize));
            int initBlock = (int) (Math.ceil((numBlocks / (double) HadoopConfiguration.getNumThreads(hadoopConf)) * threadId));
            int endBlock = (int) (Math.ceil((numBlocks / (double) HadoopConfiguration.getNumThreads(hadoopConf)) * (threadId + 1)));

            PersonGenerator personGenerator = new PersonGenerator(
                    conf,
                    DatagenParams.getDegreeDistribution().getClass().getName()
            );
            for (int i = initBlock; i < endBlock; ++i) {
                int size = (int) Math.min(DatagenParams.numPersons - DatagenParams.blockSize * i, DatagenParams.blockSize);
                Iterator<Person> personIterator = personGenerator.generatePersonBlock(i, DatagenParams.blockSize);
                for (int j = 0; j < size; ++j) {
                    try {
                        Person p = personIterator.next();
                        context.write(keySetter.getKey(p), p);
                    } catch (IOException ioE) {
                        System.err.println("Input/Output Exception when writing to context.");
                        ioE.printStackTrace();
                    } catch (InterruptedException iE) {
                        System.err.println("Interrupted Exception when writing to context.");
                        iE.printStackTrace();
                    }
                }
            }
        }
    }

    public static class HadoopPersonGeneratorReducer extends Reducer<TupleKey, Person, TupleKey, Person> {

        @Override
        public void reduce(TupleKey key, Iterable<Person> valueSet,
                           Context context) throws IOException, InterruptedException {
            for (Person person : valueSet) {
                context.write(key, person);
            }
        }
    }

    private static void writeToOutputFile(String filename, int numMaps, Configuration conf) {
        try {
            FileSystem dfs = FileSystem.get(conf);
            OutputStream output = dfs.create(new Path(filename));
            for (int i = 0; i < numMaps; i++)
                output.write((i + "\n").getBytes());
            output.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a Person hadoop sequence file containing key-value pairs
     * where the key is the person id and the value is the person itself.
     *
     * @param outputFileName The name of the file to store the persons.
     * @throws Exception exception
     */
    public void run(String outputFileName, String postKeySetterName) throws Exception {

        String buildDir = conf.getBuildDir();
        int numThreads = HadoopConfiguration.getNumThreads(hadoopConf);
        String tempFile = buildDir + "/mrInputFile";

        FileSystem dfs = FileSystem.get(hadoopConf);
        dfs.delete(new Path(tempFile), true);
        writeToOutputFile(tempFile, numThreads, hadoopConf);

        hadoopConf.setInt("mapreduce.input.lineinputformat.linespermap", 1);
        hadoopConf.set("postKeySetterName", postKeySetterName);
        Job job = Job.getInstance(hadoopConf, "SIB Generate Persons & 1st Dimension");
        job.setMapOutputKeyClass(TupleKey.class);
        job.setMapOutputValueClass(Person.class);
        job.setOutputKeyClass(TupleKey.class);
        job.setOutputValueClass(Person.class);
        job.setJarByClass(HadoopPersonGeneratorMapper.class);
        job.setMapperClass(HadoopPersonGeneratorMapper.class);
        job.setReducerClass(HadoopPersonGeneratorReducer.class);
        job.setNumReduceTasks(numThreads);
        job.setInputFormatClass(NLineInputFormat.class);
        job.setOutputFormatClass(SequenceFileOutputFormat.class);
        FileInputFormat.setInputPaths(job, new Path(tempFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFileName));
        if (!job.waitForCompletion(true)) {
            throw new IllegalStateException("HadoopPersonGenerator failed");
        }
    }
}
