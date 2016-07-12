package com.magnusjason.githubprstats;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class CommitNode {

    @GraphId
    Long id;

    Integer size;
    Integer files;
    String message;
    Long timestamp;
}
