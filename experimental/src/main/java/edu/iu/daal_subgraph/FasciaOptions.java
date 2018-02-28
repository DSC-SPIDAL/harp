/*
 * Copyright 2013-2017 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.iu.daal_subgraph;

import org.apache.commons.cli.*;

public class FasciaOptions {

    class OPTS{
        //options
        public static final String HELP_OPTION = "h";
        public static final String LABEL_OPTION = "l";
        public static final String GRAPH_FILE_OPTION ="g";
        public static final String TEMPLATE_FILE_OPTION = "t";
        public static final String BATCH_FILE_OPTION = "b";
        public static final String ITERATIONS_OPTION = "i";
        public static final String MOTIF_OPTION ="m";
        public static final String CAL_AUTOMORPHISM_OPTION="a";
        public static final String DO_VERT_OPTION="c";
        public static final String DO_GDD_OPTION = "d";
        public static final String DO_OUTERLOOP_OPTION = "O";
        public static final String VERBOSE_OPTION_OPTION = "v";
        public static final String TIMING_OPTION="r";
        public static final String QUESTION_OPTION="?";
        public static final String THREAD_NUM_OPTION="thread";
        public static final String CORE_NUM_OPTION="core";
        public static final String AFFINITY_OPTION="affinity";
    }


    String graph_file = null;
    String template_file = null;
    String batch_file = null;
    int iterations = 1;
    boolean do_outerloop = false;
    boolean calculate_automorphism = true;
    boolean labeled = false;
    boolean do_gdd = false;
    boolean do_vert = false;
    boolean verbose = false;
    int motif = 0;
    boolean timing;
    int thread_num = 1;
    int core_num = 12;
    String thd_affinity = "compact";

    public void print_info_short(){
        System.out.println("\nTo run: fascia [-g graphfile] [-t template || -b batchfile] [options]");
        System.out.println("Help: fascia -h\n\n");
        System.exit(0);
    }

    public void print_info(){
        System.out.println("\nTo run: fascia [-g graphfile] [-t template || -b batchfile] [options]");
        System.out.println("\tgraphfile = \n");
        System.out.println("\t\tn");
        System.out.println("\t\tm");
        System.out.println("\t\tv0 v1");
        System.out.println("\t\tv0 v2");
        System.out.println("\t\t...");
        System.out.println("\t\t(zero indexed)\n");

        System.out.print("\tgraphfile (if labeled) = \n");
        System.out.print("\t\tn\n");
        System.out.print("\t\tm\n");
        System.out.print("\t\tlabel_v0\n");
        System.out.print("\t\tlabel_v1\n");
        System.out.print("\t\t...\n");
        System.out.print("\t\tv0 v1\n");
        System.out.print("\t\tv0 v2\n");
        System.out.print("\t\t...\n");
        System.out.print("\t\t(zero indexed)\n\n");

        System.out.print("\ttemplate =\n");
        System.out.print("\t\tsame format as graphfile\n\n");

        System.out.print("\tbatchfile =\n");
        System.out.print("\t\ttemplate1\n");
        System.out.print("\t\ttemplate2\n");
        System.out.print("\t\t...\n");
        System.out.print("\t\t(must supply only one of template file or batchfile)\n\n");

        System.out.print("\toptions = \n");
        System.out.print("\t\t-m  [#], compute counts for motifs of size #\n");
        System.out.print("\t\t-o  Use outerloop parallelization\n");
        System.out.print("\t\t-l  Graph and template are labeled\n");
        System.out.print("\t\t-i  [# iterations], default: 1\n");
        System.out.print("\t\t-c  Output per-vertex counts to [template].vert\n");
        System.out.print("\t\t-d  Output graphlet degree distribution to [template].gdd\n")    ;
        System.out.print("\t\t-a  Do not calculate automorphism of template\n");
        System.out.print("\t\t\t(recommended when template size > 10)\n");
        System.out.print("\t\t-r  Report runtime\n");
        System.out.print("\t\t-v  Verbose output\n");
        System.out.print("\t\t-h  Print this\n\n");

        System.exit(0);


    }

    public FasciaOptions(String args[]) {

        Options options = new Options();

        options.addOption(OPTS.HELP_OPTION, false, "print help message");
        options.addOption(OPTS.LABEL_OPTION, false, "labeled or unlabeled");
        options.addOption(OPTS.GRAPH_FILE_OPTION, true, "the graph file");
        options.addOption(OPTS.TEMPLATE_FILE_OPTION, true, "the template file");
        options.addOption(OPTS.BATCH_FILE_OPTION, true, "the batch file");
        options.addOption(OPTS.ITERATIONS_OPTION, true, "number of iterations");
        options.addOption(OPTS.MOTIF_OPTION, true, "motif");
        options.addOption(OPTS.CAL_AUTOMORPHISM_OPTION, false, "calculate automorphism");
        options.addOption(OPTS.DO_VERT_OPTION, false, "do vert");
        options.addOption(OPTS.DO_GDD_OPTION, false, "do gdd");
        options.addOption(OPTS.DO_OUTERLOOP_OPTION, false, "do outer loop");
        options.addOption(OPTS.VERBOSE_OPTION_OPTION, false, "verbose");
        options.addOption(OPTS.TIMING_OPTION, false, "timing");
        options.addOption(OPTS.QUESTION_OPTION, false, "questions?");
        options.addOption(OPTS.THREAD_NUM_OPTION, true, "number of threads");
        options.addOption(OPTS.CORE_NUM_OPTION, true, "number of cores");
        options.addOption(OPTS.AFFINITY_OPTION, true, "thread affinity");

        CommandLineParser parser = new GnuParser();
        HelpFormatter formatter = new HelpFormatter();

        try {

            CommandLine line = parser.parse(options, args);

            if(line.hasOption(OPTS.HELP_OPTION)){
                print_info();
                System.exit(0);
            }

            if( args.length < 2) {
                print_info_short();
                System.exit(0);
            }

            if(line.hasOption(OPTS.LABEL_OPTION)){
                labeled = true;
            }

            if( line.hasOption(OPTS.GRAPH_FILE_OPTION)){
                graph_file = line.getOptionValue(OPTS.GRAPH_FILE_OPTION);
            }else{
                System.err.println("Must supply graph file");
                System.exit(0);
            }

            if( line.hasOption(OPTS.TEMPLATE_FILE_OPTION) ){
                template_file = line.getOptionValue(OPTS.TEMPLATE_FILE_OPTION);
            }

            if( line.hasOption(OPTS.BATCH_FILE_OPTION) ){
                batch_file = line.getOptionValue(OPTS.BATCH_FILE_OPTION);
            }

            if( template_file == null && batch_file == null && motif == 0){
                System.err.println("Must supply template XOR batchfile or -m option");
                System.exit(0);
            }

            if( template_file != null && batch_file != null){

                System.err.println("Must only supply template file XOR batch file" );
                System.exit(0);
            }

            if( line.hasOption(OPTS.ITERATIONS_OPTION)){
                iterations = Integer.parseInt( line.getOptionValue(OPTS.ITERATIONS_OPTION));
            }

            if( iterations < 1){

                System.err.println("Number of iterations must be positive");
                System.exit(0);
            }


            if( line.hasOption(OPTS.MOTIF_OPTION)){
                motif = Integer.parseInt( line.getOptionValue(OPTS.MOTIF_OPTION));
                if( motif < 3 || motif > 10){
                    System.err.println("Motif option must be between [3,10]");
                    System.exit(0);
                }
            }

            if( line.hasOption(OPTS.CAL_AUTOMORPHISM_OPTION)){
                calculate_automorphism = false;
            }

            if( line.hasOption(OPTS.DO_VERT_OPTION)){
                do_vert = true;
            }
            if(line.hasOption(OPTS.DO_GDD_OPTION)){
                do_gdd = true;
            }
            if( line.hasOption(OPTS.DO_OUTERLOOP_OPTION)){
                do_outerloop = true;
            }
            if(line.hasOption(OPTS.VERBOSE_OPTION_OPTION)){
                verbose = true;
            }
            if(line.hasOption(OPTS.TIMING_OPTION)){
                timing = true;
            }

            if(line.hasOption(OPTS.THREAD_NUM_OPTION)){
                thread_num = Integer.parseInt(line.getOptionValue(OPTS.THREAD_NUM_OPTION));
            }

            if(line.hasOption(OPTS.CORE_NUM_OPTION)){
                core_num = Integer.parseInt(line.getOptionValue(OPTS.CORE_NUM_OPTION));
            }

            if(line.hasOption(OPTS.AFFINITY_OPTION))
            {
                thd_affinity = line.getOptionValue(OPTS.AFFINITY_OPTION);
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }



}
