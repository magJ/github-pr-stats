package com.magnusjason.githubprstats;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

@NodeEntity
public class CommentNode {

    @GraphId
    Long id;

    Long number;
    Integer position;
    Integer line;
    Long createdAt;
    Long updatedAt;
    String path;
    String body;

    @RelatedTo(type="AUTHOR", direction = Direction.BOTH)
    UserNode author;

}
