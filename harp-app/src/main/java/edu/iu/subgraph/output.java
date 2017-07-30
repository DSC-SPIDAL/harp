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
package edu.iu.subgraph;

import java.util.List;

public class output {
    private double[] vert_counts;
    private int num_verts;

    public output(double[] vc, int nv){
        vert_counts = vc;
        num_verts = nv;
    }

    public output(double[][] vc, int num_verts_counts, int nv){
        num_verts = nv;
        vert_counts = vc[0];

        for( int v = 0; v < num_verts; ++v){
            for(int i = 1; v < num_verts_counts; ++i){
                vert_counts[v] += vc[i][v];
            }
            vert_counts[v] /= num_verts_counts;
        }
    }

    public void output_gdd(String output_filename){}
    public void write_gdd(String output_filename, List gdd){}
    public void output_verts(String output_filename){}

    private int get_count_index(List gdd, double count){ return 0;}
    private void do_gdd_sort(List gdd){};

}
