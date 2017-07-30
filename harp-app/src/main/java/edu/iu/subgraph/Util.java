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

import java.util.ArrayList;
import java.util.List;

public class Util {

    public static int choose(int n, int k){

        if( n < k){
            return 0;
        }else{
            return factorial(n) / (factorial(k) * factorial(n - k));
        }
    }

    public static int factorial(int x){
        if( x <= 0){
            return 1;
        }else{
            return (x == 1 ? x: x * factorial(x -1));
        }
    }

    public static int get_max(List<Integer> arr1, List<Integer> arr2){
        int maximum = 0;
        int size = arr1.size();

        for(int i =0;  i < size; i++){
            if( maximum < arr1.get(i)){
                maximum = arr1.get(i);
            }
            if( maximum < arr2.get(i)){
                maximum = arr2.get(i);
            }
        }
        return maximum;
    }

    public static int[] dynamic_to_static(List<Integer> integers)
    {
        int[] ret = new int[integers.size()];
        for (int i=0; i < ret.length; i++)
        {
            ret[i] = integers.get(i).intValue();
        }
        return ret;
    }

    public static void next_set(int[] current_set, int length, int num_colors){
        for(int i = length-1; i>=0; --i){
            if( current_set[i] < num_colors - (length-i-1)){
                current_set[i] = current_set[i] + 1;
                for(int j = i + 1; j < length; ++j){
                    current_set[j] = current_set[j-1] + 1;
                }
                break;
            }
        }
    }

    public static int[] init_permutation(int num_verts){
        int[] perm = new int[num_verts];
        for(int i = 0; i < num_verts; ++i){
            perm[i] = i + 1;
        }
        return perm;
    }

    public static int[][] init_choose_table(int num_colors){
        int[][] choose_table = new int[num_colors + 1][num_colors+1];

        for(int i = 0; i <= num_colors; ++i){
            for(int j = 0; j <= num_colors; ++j){
                choose_table[i][j] = choose(i,j);
            }
        }

        return choose_table;
    }

    public static int get_color_index(int[] colorset, int length){
        int count = 0;
        for(int i = 0; i < length; ++i){
            int n = colorset[i] - 1;
            int k = i + 1;
            count += choose(n, k);
        }
        return count;
    }

    public static int count_automorphisms(Graph t){
        int count = 0;
        int num_verts = t.num_vertices();

        List<Integer> mapping = new ArrayList<>();
        List<Integer> rest = new ArrayList<>();
        for(int i = 0; i < num_verts; ++i){
            rest.add(i);
        }

        return count = count_all_automorphisms(t, mapping, rest);
    }

    public static int count_all_automorphisms(Graph t, List<Integer> mapping, List<Integer> rest){
        int count = 0;
        if( rest.size() == 0){
            count = test_automorphism(t, mapping);
            return count;

        }else{
            for(int i = 0; i < rest.size(); ++i){
                mapping.add(rest.get(i));
                List<Integer> new_rest = new ArrayList<>();

                for(int j = 0; j < rest.size(); ++j){
                    if( i != j)
                        new_rest.add(rest.get(j));
                }

                count += count_all_automorphisms(t, mapping, new_rest);
                new_rest.clear();
                mapping.remove(mapping.size()-1);
            }
        }
        return count;
    }

    public static int test_automorphism(Graph t, List<Integer> mapping){
        for(int v = 0; v < mapping.size(); ++v){
            if( t.out_degree(v) != t.out_degree(mapping.get(v)))
                return 0;
            else{
                int[] adjs = t.adjacent_vertices(v);
                int[] adjs_map = t.adjacent_vertices(mapping.get(v));
                int end = t.out_degree(v);

                boolean[] match = new boolean[end];

                for(int i = 0; i < end; ++i){
                    match[i] = false;
                    int u = adjs[i];

                    for(int j = 0; j < end; ++j){
                        int u_map = adjs_map[j];
                        if( u == mapping.get(u_map))
                            match[i] = true;
                    }
                }
                for(int i = 0; i < end; ++i){
                    if( !match[i])
                        return 0;
                }
            }
        }

        return 1;
    }


}
