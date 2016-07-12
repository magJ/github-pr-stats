package com.magnusjason.githubprstats;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class UserNode {

    @GraphId
    Long id;

    String userName;
    String name;


}
