package org.apache.shardingsphere.infra.optimize.rel.physical;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelCollationTraitDef;
import org.apache.calcite.rel.RelDistributionTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Calc;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.rel.metadata.RelMdCollation;
import org.apache.calcite.rel.metadata.RelMdDistribution;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexProgram;
import org.apache.shardingsphere.infra.optimize.planner.ShardingSphereConvention;

import java.util.Collections;
import java.util.List;

public class SSCalc extends Calc implements SSRel {
    protected SSCalc(final RelOptCluster cluster, final RelTraitSet traits, final List<RelHint> hints, final RelNode child, final RexProgram program) {
        super(cluster, traits, hints, child, program);
    }
    
    @Override
    public Calc copy(final RelTraitSet traitSet, final RelNode child, final RexProgram program) {
        return new SSCalc(this.getCluster(), traitSet, Collections.emptyList(), child, program);
    }
    
    public static SSCalc create(RelNode input, RexProgram program) {
        RelOptCluster cluster = input.getCluster();
        RelMetadataQuery mq = cluster.getMetadataQuery();
        RelTraitSet traitSet = cluster.traitSet()
                .replace(ShardingSphereConvention.INSTANCE)
                .replaceIfs(RelCollationTraitDef.INSTANCE, () -> RelMdCollation.calc(mq, input, program))
                .replaceIf(RelDistributionTraitDef.INSTANCE, () -> RelMdDistribution.calc(mq, input, program));
        return new SSCalc(cluster, traitSet, Collections.emptyList(), input, program);
    }
}