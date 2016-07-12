package com.magnusjason.githubprstats;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

@NodeEntity
public class RepositoryNode {

    @GraphId
    Long id;
    String name;
    String language;

    @RelatedTo(type="OWNER", direction = Direction.BOTH)
    UserNode owner;

    @RelatedTo(type="PULL_REQUEST", direction = Direction.BOTH)
    Set<PullRequestNode> pullRequests = new HashSet<>();


}
