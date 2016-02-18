package ldbc.snb.datagen.test;

import ldbc.snb.datagen.test.csv.*;
import org.codehaus.groovy.vmplugin.v5.JUnit4Utils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by aprat on 18/12/15.
 */
public class LDBCDatagenTest {

    final String dir = "./test_data/social_network";

    @BeforeClass
    public static void generateData() {
        ProcessBuilder pb = new ProcessBuilder("java", "-cp", "ldbc_snb_datagen.jar","org.apache.hadoop.util.RunJar","./ldbc_snb_datagen.jar","./test_params.ini");
        pb.directory(new File("./"));
        try {
            Process p = pb.start();
            p.waitFor();

        }catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }

    @Test
    public void personTest() {
        testIdUniqueness(dir+"/person_0_0.csv", 0);
    }

    @Test
    public void postTest() {
        testIdUniqueness(dir+"/post_0_0.csv", 0);
    }

    @Test
    public void forumTest() {
        testIdUniqueness(dir+"/forum_0_0.csv", 0);
    }

    @Test
    public void commentTest() {
        testIdUniqueness(dir+"/comment_0_0.csv", 0);
    }

    @Test
    public void organisationTest() {
        testIdUniqueness(dir+"/organisation_0_0.csv", 0);
    }

    @Test
    public void placeTest() {
        testIdUniqueness(dir+"/place_0_0.csv", 0);
    }

    @Test
    public void tagTest() {
        testIdUniqueness(dir+"/tag_0_0.csv", 0);
    }

    @Test
    public void tagclassTest() {
        testIdUniqueness(dir+"/tagclass_0_0.csv", 0);
    }

    @Test
    public void personKnowsPersonTest() {
        testPairUniquenessPlusExistance(dir+"/person_knows_person_0_0.csv",0,1,dir+"/person_0_0.csv",0);
    }

    @Test
    public void organisationIsLocatedInPlaceTest() {
        testPairUniquenessPlusExistance(dir+"/organisation_isLocatedIn_place_0_0.csv",0,1,dir+"/organisation_0_0.csv",0,dir+"/place_0_0.csv",0);
    }

    @Test
    public void placeIsPartOfPlaceTest() {
        testPairUniquenessPlusExistance(dir+"/place_isPartOf_place_0_0.csv",0,1,dir+"/place_0_0.csv",0);
    }

    @Test
    public void tagClassIsSubclassOfTest() {
        testPairUniquenessPlusExistance(dir+"/tagclass_isSubclassOf_tagclass_0_0.csv",0,1,dir+"/tagclass_0_0.csv",0);
    }

    @Test
    public void tagHasTypeTagclassCheck() {
        testPairUniquenessPlusExistance(dir+"/tag_hasType_tagclass_0_0.csv",0,1,dir+"/tag_0_0.csv",0,dir+"/tagclass_0_0.csv",0);
    }

    @Test
    public void personStudyAtOrganisationCheck() {
        testPairUniquenessPlusExistance(dir+"/person_studyAt_organisation_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/organisation_0_0.csv",0);
    }

    @Test
    public void personWorkAtOrganisationCheck() {
        testPairUniquenessPlusExistance(dir+"/person_workAt_organisation_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/organisation_0_0.csv",0);
    }

    @Test
    public void personHasInterestTagCheck() {
        testPairUniquenessPlusExistance(dir+"/person_hasInterest_tag_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/tag_0_0.csv",0);
    }

    @Test
    public void personIsLocatedInPlaceCheck() {
        testPairUniquenessPlusExistance(dir+"/person_isLocatedIn_place_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/place_0_0.csv",0);
    }

    @Test
    public void forumHasTagCheck() {
        testPairUniquenessPlusExistance(dir+"/forum_hasTag_tag_0_0.csv",0,1,dir+"/forum_0_0.csv",0,dir+"/tag_0_0.csv",0);
    }

    @Test
    public void forumHasModeratorPersonCheck() {
        testPairUniquenessPlusExistance(dir+"/forum_hasModerator_person_0_0.csv",0,1,dir+"/forum_0_0.csv",0,dir+"/person_0_0.csv",0);
    }

    @Test
    public void forumHasMemberPersonCheck() {
        testPairUniquenessPlusExistance(dir+"/forum_hasMember_person_0_0.csv",0,1,dir+"/forum_0_0.csv",0,dir+"/person_0_0.csv",0);
    }

    @Test
    public void forumContainerOfPostCheck() {
        testPairUniquenessPlusExistance(dir+"/forum_containerOf_post_0_0.csv",0,1,dir+"/forum_0_0.csv",0,dir+"/post_0_0.csv",0);
    }

    @Test
    public void commentHasCreatorPersonCheck() {
        testPairUniquenessPlusExistance(dir+"/comment_hasCreator_person_0_0.csv",0,1,dir+"/comment_0_0.csv",0,dir+"/person_0_0.csv",0);
    }

    @Test
    public void commentHasTagTagCheck() {
        testPairUniquenessPlusExistance(dir+"/comment_hasTag_tag_0_0.csv",0,1,dir+"/comment_0_0.csv",0,dir+"/tag_0_0.csv",0);
    }

    @Test
    public void commentIsLocatedInPlaceCheck() {
        testPairUniquenessPlusExistance(dir+"/comment_isLocatedIn_place_0_0.csv",0,1,dir+"/comment_0_0.csv",0,dir+"/place_0_0.csv",0);
    }

    @Test
    public void commentReplyOfCommentCheck() {
        testPairUniquenessPlusExistance(dir+"/comment_replyOf_comment_0_0.csv",0,1,dir+"/comment_0_0.csv",0,dir+"/comment_0_0.csv",0);
    }

    @Test
    public void commentReplyOfPostCheck() {
        testPairUniquenessPlusExistance(dir+"/comment_replyOf_post_0_0.csv",0,1,dir+"/comment_0_0.csv",0,dir+"/post_0_0.csv",0);
    }

    @Test
    public void postHasCreatorPersonCheck() {
        testPairUniquenessPlusExistance(dir+"/post_hasCreator_person_0_0.csv",0,1,dir+"/post_0_0.csv",0,dir+"/person_0_0.csv",0);
    }

    @Test
    public void postIsLocatedInPlaceCheck() {
        testPairUniquenessPlusExistance(dir+"/post_isLocatedIn_place_0_0.csv",0,1,dir+"/post_0_0.csv",0,dir+"/place_0_0.csv",0);
    }

    @Test
    public void personLikesCommentCheck() {
        testPairUniquenessPlusExistance(dir+"/person_likes_comment_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/comment_0_0.csv",0);
    }

    @Test
    public void personLikesPostCheck() {
        testPairUniquenessPlusExistance(dir+"/person_likes_post_0_0.csv",0,1,dir+"/person_0_0.csv",0,dir+"/post_0_0.csv",0);
    }

    public void testIdUniqueness(String fileName, int column) {
        FileChecker fileChecker = new FileChecker(fileName);
        UniquenessCheck check = new UniquenessCheck(0);
        fileChecker.addCheck(check);
        if(!fileChecker.run(1)) assertEquals("ERROR PASSING TEST ID UNIQUENESS FOR FILE "+fileName,true, false);
    }

    public void testPairUniquenessPlusExistance(String relationFileName, int columnA, int columnB, String entityFileNameA, int entityColumnA, String entityFileNameB, int entityColumnB) {
        LongParser parser = new LongParser();
        ColumnSet<Long> entitiesA = new ColumnSet<Long>(parser,new File(entityFileNameA),entityColumnA,1);
        entitiesA.initialize();
        ColumnSet<Long> entitiesB = new ColumnSet<Long>(parser,new File(entityFileNameB),entityColumnB,1);
        entitiesB.initialize();
        FileChecker fileChecker = new FileChecker(relationFileName);
        PairUniquenessCheck pairUniquenessCheck = new PairUniquenessCheck<Long,Long>(parser,parser,columnA,columnB);
        fileChecker.addCheck(pairUniquenessCheck);
        List<ColumnSet<Long>> entityARefColumns = new ArrayList<ColumnSet<Long>>();
        entityARefColumns.add(entitiesA);
        List<ColumnSet<Long>> entityBRefColumns = new ArrayList<ColumnSet<Long>>();
        entityBRefColumns.add(entitiesB);
        List<Integer> organisationIndices = new ArrayList<Integer>();
        organisationIndices.add(columnA);
        List<Integer> placeIndices = new ArrayList<Integer>();
        placeIndices.add(columnB);
        ExistsCheck<Long> existsEntityACheck = new ExistsCheck<Long>(parser,organisationIndices, entityARefColumns);
        ExistsCheck<Long> existsEntityBCheck = new ExistsCheck<Long>(parser,placeIndices, entityBRefColumns);
        fileChecker.addCheck(existsEntityACheck);
        fileChecker.addCheck(existsEntityBCheck);
        assertEquals("ERROR PASSING ORGANISATION_ISLOCATEDIN_PLACE TEST",true, fileChecker.run(1));

    }

    public void testPairUniquenessPlusExistance(String relationFileName, int columnA, int columnB, String entityFileName, int entityColumn) {
        LongParser parser = new LongParser();
        ColumnSet<Long> entities = new ColumnSet<Long>(parser,new File(entityFileName),entityColumn,1);
        entities.initialize();
        FileChecker fileChecker = new FileChecker(relationFileName);
        PairUniquenessCheck pairUniquenessCheck = new PairUniquenessCheck<Long,Long>(parser,parser,columnA,columnB);
        fileChecker.addCheck(pairUniquenessCheck);
        List<ColumnSet<Long>> refcolumns = new ArrayList<ColumnSet<Long>>();
        refcolumns.add(entities);
        List<Integer> columnIndices = new ArrayList<Integer>();
        columnIndices.add(columnA);
        columnIndices.add(columnB);
        ExistsCheck existsCheck = new ExistsCheck<Long>(parser,columnIndices, refcolumns);
        fileChecker.addCheck(existsCheck);
        assertEquals("ERROR PASSING "+relationFileName+" TEST",true, fileChecker.run(1));
    }

}