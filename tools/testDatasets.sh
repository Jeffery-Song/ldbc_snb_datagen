#!/bin/bash

if [ $# -ne 2 ]
then
   echo "Arguments not correctly supplied"
   echo "Usage: sh testDatasets <num_reducers1> <dir1> <num_reducers2> <dir2>"
   exit
fi

FILES="person person_email_emailaddress  person_studyAt_organization person_hasInterest_tag person_workAt_organization person_isLocatedIn_place  person_knows_person person_speaks_language tag tagclass_isSubClassOf_tagclass tag_hasType_tagclass tagclass place place_isPartOf_place organization organization_isLocatedIn_place"

FILES2="post comment forum forum_hasTag_tag forum_hasModerator_person forum_hasMember_person forum_containerOf_post comment_hasCreator_person comment_hasTag_tag comment_isLocatedIn_place comment_replyOf_comment comment_replyOf_post post_hasCreator_person post_hasTag_tag post_isLocatedIn_place person_likes_comment person_likes_post"

FILES="$FILES $FILES2"

DIR_1=$1
DIR_2=$2

for file in $FILES
do
   echo "CHECKING FILE $file"
   $(sort $DIR_1/${file}_?_?.csv  > .auxFile1)
   $(sort $DIR_2/${file}_?_?.csv  > .auxFile2)

   # computing checksums
   a=$(md5sum .auxFile1 | awk '{print $1}')
   b=$(md5sum .auxFile2 | awk '{print $1}')
    
   if [ "$a" == "$b" ] 
   then
       echo ${file} are equal 
       echo ${a} 
       echo ${b} 
   else
       echo ERROR!!!!! ${file} are different 
       echo ${a} 
       echo ${b} 
       exit
   fi
   echo "---------------------"
done
echo GREAT!!!!! the two datasets are the same! 
rm -f .auxFile1
rm -f .auxFile2


