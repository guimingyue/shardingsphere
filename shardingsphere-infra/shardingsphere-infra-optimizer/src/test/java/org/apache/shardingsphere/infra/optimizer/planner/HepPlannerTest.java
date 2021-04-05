/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.infra.optimizer.planner;

import com.google.common.collect.ImmutableList;
import org.apache.calcite.config.Lex;
import org.apache.calcite.plan.RelOptCostImpl;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgram;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.sql.SqlExplainFormat;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.optimizer.converter.RelNodeConverter;
import org.apache.shardingsphere.infra.optimizer.schema.AbstractSchemaTest;
import org.junit.Before;
import org.junit.Test;

public class HepPlannerTest extends AbstractSchemaTest {
    
    RelNodeConverter relNodeConverter;
    
    @Before
    public void init() {
        ShardingSphereSchema schema = buildSchema();
        relNodeConverter = new RelNodeConverter("logical_db", schema);
    }
    
    @Test
    public void test() throws SqlParseException {
        String sql = "select o1.order_id, o1.order_id, o1.user_id, o2.status from t_order o1 join t_order_item o2 on "
                + "o1.order_id = o2.order_id where o1.status='FINISHED' and o2.order_item_id > 1024 and 1=1";
        SqlParser parser = SqlParser.create(sql, SqlParser.config().withLex(Lex.MYSQL));
        SqlNode sqlNode = parser.parseQuery();
    
        RelNode logicalRelNode = relNodeConverter.validateAndConvert(sqlNode);
        
        HepProgramBuilder hepProgramBuilder = HepProgram.builder();
        hepProgramBuilder.addGroupBegin();
        hepProgramBuilder.addRuleCollection(ImmutableList.of(CoreRules.FILTER_INTO_JOIN));
        hepProgramBuilder.addGroupEnd();
    
        String planStr;
        planStr = RelOptUtil.dumpPlan("origin", logicalRelNode, SqlExplainFormat.TEXT, SqlExplainLevel.EXPPLAN_ATTRIBUTES);
        System.out.println(planStr);
        
        HepPlanner hepPlanner = new HepPlanner(hepProgramBuilder.build(), null, true, null, RelOptCostImpl.FACTORY);
        hepPlanner.setRoot(logicalRelNode);
        RelNode best = hepPlanner.findBestExp();
        planStr = RelOptUtil.dumpPlan("best", best, SqlExplainFormat.TEXT, SqlExplainLevel.EXPPLAN_ATTRIBUTES);
        System.out.println(planStr);
    }
}
