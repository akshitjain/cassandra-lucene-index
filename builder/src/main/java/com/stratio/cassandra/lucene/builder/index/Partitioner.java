/*
 * Copyright (C) 2014 Stratio (http://stratio.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.lucene.builder.index;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.stratio.cassandra.lucene.builder.JSONBuilder;
import com.stratio.cassandra.lucene.builder.index.Partitioner.None;
import com.stratio.cassandra.lucene.builder.index.Partitioner.OnColumn;
import com.stratio.cassandra.lucene.builder.index.Partitioner.OnToken;
import com.stratio.cassandra.lucene.builder.index.Partitioner.OnVirtualNode;

/**
 * An index partitioner to split the index in multiple partitions.
 *
 * Index partitioning is useful to speed up some searches to the detriment of others, depending on the implementation.
 *
 * It is also useful to overcome the  Lucene's hard limit of 2147483519 documents per local index.
 *
 * @author Andres de la Pena {@literal <adelapena@stratio.com>}
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", defaultImpl = None.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = None.class, name = "none"),
        @JsonSubTypes.Type(value = OnToken.class, name = "token"),
        @JsonSubTypes.Type(value = OnColumn.class, name = "column"),
        @JsonSubTypes.Type(value = OnVirtualNode.class, name = "vnode")})
public abstract class Partitioner extends JSONBuilder {

    /**
     * {@link Partitioner} with no action, equivalent to not defining a partitioner.
     */
    public static class None extends Partitioner {
    }

    /**
     * A {@link Partitioner} based on the partition key token. Rows will be stored in an index partition determined by
     * the hash of the partition key token. Partition-directed searches will be routed to a single partition, increasing
     * performance. However, token range searches will be routed to all the partitions, with a slightly lower
     * performance.
     *
     * This partitioner guarantees an excellent load balancing between index partitions.
     *
     * The number of partitions per node should be specified.
     */
    public static class OnToken extends Partitioner {

        /** The number of partitions per node. */
        @JsonProperty("partitions")
        public final int partitions;

        /** The paths where to save partitions. */
        @JsonProperty("paths")
        public String[] paths;

        /**
         * Builds a new partitioner on token with the specified number of partitions per node.
         *
         * @param partitions the number of index partitions per node
         */
        public OnToken(int partitions) {
            this.partitions = partitions;
        }

        /**
         * @param paths the paths where to save partitions
         * @return this with the specified path directories
         */
        public OnToken paths(String[] paths) {
            this.paths = paths;
            return this;
        }

    }

    /**
     * A {@link Partitioner} based on a partition key column. Rows will be stored in an index partition determined by
     * the hash of the specified partition key column. Both partition-directed as well as token range searches
     * containing an CQL equality filter over the selected partition key column will be routed to a single partition,
     * increasing performance. However, token range searches without filters over the partitioning column will be routed
     * to all the partitions, with a slightly lower performance.
     *
     * Load balancing depends on the cardinality and distribution of the values of the partitioning column. Both high
     * cardinalities and uniform distributions will provide better load balancing between partitions.
     *
     * Both the number of partitions per node and the name of the partition column should be specified.
     */
    public static class OnColumn extends Partitioner {

        /** The number of index partitions per node. */
        @JsonProperty("partitions")
        public final int partitions;

        /** The partition key column. */
        @JsonProperty("column")
        public final String column;

        /** The paths where to save partitions. */
        @JsonProperty("paths")
        public String[] paths;

        /**
         * Builds a new partitioner on the specified column with the specified number of partitions per node.
         *
         * @param partitions the number of index partitions per node
         * @param column the partition key column
         */
        public OnColumn(int partitions, String column) {
            this.partitions = partitions;
            this.column = column;
        }

        /**
         * @param paths the paths where to save partitions
         * @return this with the specified path directories
         */
        public OnColumn paths(String[] paths) {
            this.paths = paths;
            return this;
        }
    }

    /**
     * A {@link Partitioner} based on the partition key token. Rows will be stored in an index partition determined by
     * the virtual node token range. Partition-directed searches will be routed to a single partition, increasing
     * performance. However, unbounded token range searches will be routed to all the partitions, with a slightly lower
     * performance. Virtual node token range queries will be routed to only one partition which increase performance in
     * spark queries with virtual nodes rather than partitioning on token.
     *
     * This partitioner load balance depends on virtual node token ranges assignation. The more virtual nodes, the
     * better distribution (more similarity in number of tokens that falls inside any virtual node) between virtual
     * nodes, the better load balance with this partitioner.
     *
     * The number of virtual nodes per each partition must be specified.
     */
    public static class OnVirtualNode extends Partitioner {

        /** The number of partitions per node. */
        @JsonProperty("vnodes_per_partition")
        public final int virtualNodesPerPartition;

        /**
         * Builds a new virtual node based partitioner.
         *
         * @param virtualNodesPerPartition the number of virtual nodes per each partition
         */
        public OnVirtualNode(int virtualNodesPerPartition) {
            this.virtualNodesPerPartition = virtualNodesPerPartition;
        }
    }
}
