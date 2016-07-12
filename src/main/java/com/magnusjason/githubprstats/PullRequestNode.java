package com.magnusjason.githubprstats;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

@NodeEntity
public class PullRequestNode {

    @GraphId
    Long id;

    String title;
    Integer number;
    Long createdAt;
    Long closedAt;
    Long updatedAt;

    @RelatedTo(type="AUTHOR", direction = Direction.BOTH)
    UserNode author;

    @RelatedTo(type="COMMENT", direction = Direction.BOTH)
    Set<CommentNode> comments = new HashSet<>();
    @RelatedTo(type="COMMIT", direction = Direction.BOTH)
    Set<CommitNode> commits = new HashSet<>();

}
