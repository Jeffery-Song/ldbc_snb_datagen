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
package ldbc.snb.datagen.generator.generators.knowsgenerators;

import ldbc.snb.datagen.DatagenParams;
import ldbc.snb.datagen.entities.dynamic.person.Person;
import ldbc.snb.datagen.entities.dynamic.relations.Knows;
import ldbc.snb.datagen.util.LdbcConfiguration;
import ldbc.snb.datagen.util.RandomGeneratorFarm;

import java.util.List;

public class DistanceKnowsGenerator implements KnowsGenerator {

    private RandomGeneratorFarm randomFarm;

    public DistanceKnowsGenerator() {
        this.randomFarm = new RandomGeneratorFarm();
    }

    public void generateKnows(List<Person> persons, int blockId, List<Float> percentages, int step_index, Person.PersonSimilarity personSimilarity) {
        randomFarm.resetRandomGenerators(blockId);
        for (int i = 0; i < persons.size(); ++i) {
            Person p = persons.get(i);
            for (int j = i + 1; (Knows.targetEdges(p, percentages, step_index) > p.getKnows().size()) && (j < persons
                    .size()); ++j) {
                if (know(p, persons.get(j), j - i, percentages, step_index)) {
                    Knows.createKnow(
                            randomFarm.get(RandomGeneratorFarm.Aspect.DATE),
                            randomFarm.get(RandomGeneratorFarm.Aspect.DELETION_KNOWS),
                            p,
                            persons.get(j), personSimilarity);
                }
            }
        }
    }

    @Override
    public void initialize(LdbcConfiguration conf) {
        // This is inherited from knows generator and no initialization is required.
    }

    private boolean know(Person personA, Person personB, int dist, List<Float> percentages, int step_index) {
        if (personA.getKnows().size() >= Knows.targetEdges(personA, percentages, step_index) ||
                personB.getKnows().size() >= Knows.targetEdges(personB, percentages, step_index)) return false;
        double randProb = randomFarm.get(RandomGeneratorFarm.Aspect.UNIFORM).nextDouble();
        double prob = Math.pow(DatagenParams.baseProbCorrelated, dist);
        return ((randProb < prob) || (randProb < DatagenParams.limitProCorrelated));
    }

}
