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
package ldbc.snb.datagen.serializer;

import ldbc.snb.datagen.DatagenMode;
import ldbc.snb.datagen.DatagenParams;
import ldbc.snb.datagen.dictionary.Dictionaries;
import ldbc.snb.datagen.entities.dynamic.Forum;
import ldbc.snb.datagen.entities.dynamic.messages.Comment;
import ldbc.snb.datagen.entities.dynamic.messages.Photo;
import ldbc.snb.datagen.entities.dynamic.messages.Post;
import ldbc.snb.datagen.entities.dynamic.relations.ForumMembership;
import ldbc.snb.datagen.entities.dynamic.relations.Like;
import ldbc.snb.datagen.generator.generators.GenActivity;
import ldbc.snb.datagen.generator.generators.GenWall;
import ldbc.snb.datagen.hadoop.writer.HdfsCsvWriter;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.IOException;
import java.util.stream.Stream;

import static ldbc.snb.datagen.util.functional.Thunk.wrapException;

public class PersonActivityExporter implements AutoCloseable {
    protected DynamicActivitySerializer<HdfsCsvWriter> dynamicActivitySerializer;
    protected InsertEventSerializer insertEventSerializer;
    protected DeleteEventSerializer deleteEventSerializer;

    public PersonActivityExporter(DynamicActivitySerializer<HdfsCsvWriter> dynamicActivitySerializer, InsertEventSerializer insertEventSerializer, DeleteEventSerializer deleteEventSerializer) {
        this.dynamicActivitySerializer = dynamicActivitySerializer;
        this.insertEventSerializer = insertEventSerializer;
        this.deleteEventSerializer = deleteEventSerializer;
    }

    private void exportPostWall(final GenWall<Triplet<Post, Stream<Like>, Stream<Pair<Comment, Stream<Like>>>>> genWall) {
        genWall.inner.forEach(forum -> {
            wrapException(() -> this.export(forum.getValue0()));
            Stream<ForumMembership> genForumMembership = forum.getValue1();
            genForumMembership.forEach(m -> wrapException(() -> this.export(m)));
            Stream<Triplet<Post, Stream<Like>, Stream<Pair<Comment, Stream<Like>>>>> thread = forum.getValue2();
            thread.forEach(t -> {
                wrapException(() -> this.export(t.getValue0()));
                Stream<Like> genLike = t.getValue1();
                genLike.forEach(l -> wrapException(() -> this.export(l)));
                Stream<Pair<Comment, Stream<Like>>> genComment = t.getValue2();
                genComment.forEach(c -> {
                    wrapException(() -> this.export(c.getValue0()));
                    Stream<Like> genLike1 = c.getValue1();
                    genLike1.forEach(l -> wrapException(() -> this.export(l)));
                });
            });
        });
    }

    private void exportAlbumWall(final GenWall<Pair<Photo, Stream<Like>>> genAlbums) {
        genAlbums.inner.forEach(forum -> {
            wrapException(() -> this.export(forum.getValue0()));
            Stream<ForumMembership> genForumMembership = forum.getValue1();
            genForumMembership.forEach(m -> wrapException(() -> this.export(m)));
            Stream<Pair<Photo, Stream<Like>>> thread = forum.getValue2();
            thread.forEach(t -> {
                wrapException(() -> this.export(t.getValue0()));
                Stream<Like> genLike = t.getValue1();
                genLike.forEach(l -> wrapException(() -> this.export(l)));
            });
        });
    }

    public void export(final GenActivity genActivity) {
        this.exportPostWall(genActivity.genWall);
        genActivity.genGroups.forEach(this::exportPostWall);
        this.exportAlbumWall(genActivity.genAlbums);
    }

    public void export(final Forum forum) throws Exception {

        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(forum);
        } else {
            if ((forum.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (forum.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            forum.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()))) {
                dynamicActivitySerializer.export(forum);
                if (forum.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(forum);
                    deleteEventSerializer.changePartition();
                }
            } else if (forum.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && forum.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
            ) {
                dynamicActivitySerializer.export(forum);
            } else if (forum.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (forum.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    forum.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(forum);
                insertEventSerializer.changePartition();
                if (forum.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(forum);
                    deleteEventSerializer.changePartition();
                }
            } else if (forum.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && forum.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(forum);
                insertEventSerializer.changePartition();
            }
        }

    }

    public void export(final Post post) throws IOException {
        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(post);
        } else {
            if ((post.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (post.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            post.getDeletionDate() <= Dictionaries.dates.getSimulationEnd())
                    )) {
                dynamicActivitySerializer.export(post);
                if (post.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(post);
                    deleteEventSerializer.changePartition();
                }
            } else if (post.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && post.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
                    ) {
                dynamicActivitySerializer.export(post);
            } else if (post.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (post.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    post.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(post);
                insertEventSerializer.changePartition();
                if (post.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(post);
                    deleteEventSerializer.changePartition();
                }
            } else if (post.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && post.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(post);
                insertEventSerializer.changePartition();
            }
        }

    }

    public void export(final Comment comment) throws IOException {
        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(comment);
        } else {
         if ((comment.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (comment.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            comment.getDeletionDate() <= Dictionaries.dates.getSimulationEnd())
                    )) {
                dynamicActivitySerializer.export(comment);
                if (comment.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(comment);
                    deleteEventSerializer.changePartition();
                }
            } else if (comment.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && comment.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
                    ) {
                dynamicActivitySerializer.export(comment);
            } else if (comment.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (comment.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    comment.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(comment);
                insertEventSerializer.changePartition();
                if (comment.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(comment);
                    deleteEventSerializer.changePartition();
                }
            } else if (comment.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && comment.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(comment);
                insertEventSerializer.changePartition();
            }
        }
    }

    public void export(final Photo photo) throws IOException {
        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(photo);
        } else {
            if ((photo.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (photo.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            photo.getDeletionDate() <= Dictionaries.dates.getSimulationEnd())
            )) {
                dynamicActivitySerializer.export(photo);
                if (photo.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(photo);
                    deleteEventSerializer.changePartition();
                }
            } else if (photo.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && photo.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
            ) {
                dynamicActivitySerializer.export(photo);
            } else if (photo.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (photo.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    photo.getDeletionDate() <= Dictionaries.dates.getSimulationEnd() ) {
                insertEventSerializer.export(photo);
                insertEventSerializer.changePartition();
                if (photo.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(photo);
                    deleteEventSerializer.changePartition();
                }
            } else if (photo.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && photo.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(photo);
                insertEventSerializer.changePartition();
            }
        }
    }

    public void export(final ForumMembership member) throws IOException {

        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(member);
        } else {
            if ((member.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (member.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            member.getDeletionDate() <= Dictionaries.dates.getSimulationEnd())
                    )) {
                dynamicActivitySerializer.export(member);
                if (member.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(member);
                    deleteEventSerializer.changePartition();
                }
            } else if (member.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && member.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
                    ) {
                dynamicActivitySerializer.export(member);
            } else if (member.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (member.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    member.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(member);
                insertEventSerializer.changePartition();
                if (member.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(member);
                    deleteEventSerializer.changePartition();
                }
            } else if (member.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && member.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(member);
                insertEventSerializer.changePartition();
            }
        }
    }

    public void export(final Like like) throws IOException {

        if (DatagenParams.getDatagenMode() == DatagenMode.RAW_DATA){
            dynamicActivitySerializer.export(like);
        } else {
           if ((like.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold() &&
                    (like.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold() &&
                            like.getDeletionDate() <= Dictionaries.dates.getSimulationEnd())
            )) {
                dynamicActivitySerializer.export(like);
                if (like.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(like);
                    deleteEventSerializer.changePartition();
                }
            } else if (like.getCreationDate() < Dictionaries.dates.getBulkLoadThreshold()
                    && like.getDeletionDate() > Dictionaries.dates.getSimulationEnd()
            ) {
                dynamicActivitySerializer.export(like);
            } else if (like.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && (like.getDeletionDate() >= Dictionaries.dates.getBulkLoadThreshold()) &&
                    like.getDeletionDate() <= Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(like);
                insertEventSerializer.changePartition();
                if (like.isExplicitlyDeleted()) {
                    deleteEventSerializer.export(like);
                    deleteEventSerializer.changePartition();
                }
            } else if (like.getCreationDate() >= Dictionaries.dates.getBulkLoadThreshold()
                    && like.getDeletionDate() > Dictionaries.dates.getSimulationEnd()) {
                insertEventSerializer.export(like);
                insertEventSerializer.changePartition();
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (dynamicActivitySerializer != null) {
            dynamicActivitySerializer.close();
        }
        if (insertEventSerializer != null) {
            insertEventSerializer.close();
        }
        if (deleteEventSerializer != null) {
            deleteEventSerializer.close();
        }
    }
}
