create sequence hibernate_sequence start 1 increment 1

    create table bsnAttributes (
       Bundle_id int4 not null,
        symbolicNameAttributes varchar(255),
        symbolicNameAttributes_KEY varchar(255) not null,
        primary key (Bundle_id, symbolicNameAttributes_KEY)
    )

    create table Bundle (
       id  serial not null,
        description varchar(255),
        docUrl varchar(255),
        exportService varchar(32767),
        license varchar(8192),
        name varchar(255),
        symbolicName varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        exportPackage_id int8,
        importPackage_id int8,
        requireBundle_id int8,
        primary key (id)
    )

    create table MavenArtifact (
       id  serial not null,
        artifactId varchar(255) not null,
        groupId varchar(255) not null,
        version varchar(255) not null,
        primary key (id)
    )

    create table MavenResource (
       id  serial not null,
        MD5 varchar(255),
        SHA1 varchar(255),
        classifier varchar(255),
        fileExtension varchar(255),
        lastModified timestamp,
        packaging varchar(255),
        size int8,
        bundle_id int4,
        mavenArtifact_id int4 not null,
        primary key (id)
    )

    create table Package (
       DTYPE varchar(31) not null,
        id  serial not null,
        name varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        leftOpen boolean,
        rightMajor int4,
        rightMicro int4,
        rightMinor int4,
        rightQualifier varchar(255),
        rightOpen boolean,
        primary key (id)
    )

    create table Package_attrs (
       Package_id int4 not null,
        attrs varchar(32767),
        attrs_KEY varchar(255) not null,
        primary key (Package_id, attrs_KEY)
    )

    create table Parameters (
       id int8 not null,
        primary key (id)
    )

    create table Parameters_ParameterValue (
       Parameters_id int8 not null,
        primary key (Parameters_id)
    )

    create table ParameterValue (
       id int8 not null,
        key varchar(8192) not null,
        primary key (id)
    )

    create table ParameterValue_attributes (
       ParameterValue_id int8 not null,
        attributes varchar(10485760),
        attributes_KEY varchar(255) not null,
        primary key (ParameterValue_id, attributes_KEY)
    )

    alter table MavenArtifact 
       add constraint UKknaqgfuee46wervds1ul0l0at unique (groupId, artifactId, version)
create index IDXimwxia9xlgxuw73yk80pi822x on MavenResource (classifier)
create index IDXiw2r993suw3hx0yupqrr2nkm6 on MavenResource (packaging)
create index IDX1vty24ygp7xcclr5jll0ty42u on MavenResource (MD5)
create index IDX15ub99oivyck7o1iwy8lmmodj on MavenResource (SHA1)

    alter table MavenResource 
       add constraint UKswf00u3a1p4wl3mbgsnv3bdxh unique (mavenArtifact_id, classifier)

    alter table bsnAttributes 
       add constraint FKqs7m5tnxxxu9pb4hyhk3u2igj 
       foreign key (Bundle_id) 
       references Bundle

    alter table Bundle 
       add constraint FKbcpvwvwuy6ypdw6a20dgptj46 
       foreign key (exportPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKb5t1o4dalplcch7c82dxtqkos 
       foreign key (importPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKgvg8dd5f89r51tpkhvpkop9c1 
       foreign key (requireBundle_id) 
       references Parameters

    alter table MavenResource 
       add constraint FK8reuvc1exmypamexrnanuhu9x 
       foreign key (bundle_id) 
       references Bundle

    alter table MavenResource 
       add constraint FKk1vhjcsypygcmsbxhec7i2ftm 
       foreign key (mavenArtifact_id) 
       references MavenArtifact

    alter table Package_attrs 
       add constraint FK1jky5aeeyiq336jp90t7hfgib 
       foreign key (Package_id) 
       references Package

    alter table Parameters_ParameterValue 
       add constraint FK40420wnjiwwiqclfymfyrex2o 
       foreign key (parameters_id) 
       references ParameterValue

    alter table ParameterValue_attributes 
       add constraint FK8ptub2oyy40v25umjcfxf2vsd 
       foreign key (ParameterValue_id) 
       references ParameterValue
create sequence hibernate_sequence start 1 increment 1

    create table bsnAttributes (
       Bundle_id int4 not null,
        symbolicNameAttributes varchar(255),
        symbolicNameAttributes_KEY varchar(255) not null,
        primary key (Bundle_id, symbolicNameAttributes_KEY)
    )

    create table Bundle (
       id  serial not null,
        description varchar(255),
        docUrl varchar(255),
        exportService varchar(32767),
        license varchar(8192),
        name varchar(255),
        symbolicName varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        exportPackage_id int8,
        importPackage_id int8,
        requireBundle_id int8,
        primary key (id)
    )

    create table MavenArtifact (
       id  serial not null,
        artifactId varchar(255) not null,
        groupId varchar(255) not null,
        version varchar(255) not null,
        primary key (id)
    )

    create table MavenResource (
       id  serial not null,
        MD5 varchar(255),
        SHA1 varchar(255),
        classifier varchar(255),
        fileExtension varchar(255),
        lastModified timestamp,
        packaging varchar(255),
        size int8,
        bundle_id int4,
        mavenArtifact_id int4 not null,
        primary key (id)
    )

    create table Package (
       DTYPE varchar(31) not null,
        id  serial not null,
        name varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        leftOpen boolean,
        rightMajor int4,
        rightMicro int4,
        rightMinor int4,
        rightQualifier varchar(255),
        rightOpen boolean,
        primary key (id)
    )

    create table Package_attrs (
       Package_id int4 not null,
        attrs varchar(32767),
        attrs_KEY varchar(255) not null,
        primary key (Package_id, attrs_KEY)
    )

    create table Parameters (
       id int8 not null,
        primary key (id)
    )

    create table Parameters_ParameterValue (
       Parameters_id int8 not null,
        primary key (Parameters_id)
    )

    create table ParameterValue (
       id int8 not null,
        key varchar(8192) not null,
        primary key (id)
    )

    create table ParameterValue_attributes (
       ParameterValue_id int8 not null,
        attributes varchar(10485760),
        attributes_KEY varchar(255) not null,
        primary key (ParameterValue_id, attributes_KEY)
    )

    alter table MavenArtifact 
       add constraint UKknaqgfuee46wervds1ul0l0at unique (groupId, artifactId, version)
create index IDXimwxia9xlgxuw73yk80pi822x on MavenResource (classifier)
create index IDXiw2r993suw3hx0yupqrr2nkm6 on MavenResource (packaging)
create index IDX1vty24ygp7xcclr5jll0ty42u on MavenResource (MD5)
create index IDX15ub99oivyck7o1iwy8lmmodj on MavenResource (SHA1)

    alter table MavenResource 
       add constraint UKswf00u3a1p4wl3mbgsnv3bdxh unique (mavenArtifact_id, classifier)

    alter table bsnAttributes 
       add constraint FKqs7m5tnxxxu9pb4hyhk3u2igj 
       foreign key (Bundle_id) 
       references Bundle

    alter table Bundle 
       add constraint FKbcpvwvwuy6ypdw6a20dgptj46 
       foreign key (exportPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKb5t1o4dalplcch7c82dxtqkos 
       foreign key (importPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKgvg8dd5f89r51tpkhvpkop9c1 
       foreign key (requireBundle_id) 
       references Parameters

    alter table MavenResource 
       add constraint FK8reuvc1exmypamexrnanuhu9x 
       foreign key (bundle_id) 
       references Bundle

    alter table MavenResource 
       add constraint FKk1vhjcsypygcmsbxhec7i2ftm 
       foreign key (mavenArtifact_id) 
       references MavenArtifact

    alter table Package_attrs 
       add constraint FK1jky5aeeyiq336jp90t7hfgib 
       foreign key (Package_id) 
       references Package

    alter table Parameters_ParameterValue 
       add constraint FK40420wnjiwwiqclfymfyrex2o 
       foreign key (parameters_id) 
       references ParameterValue

    alter table ParameterValue_attributes 
       add constraint FK8ptub2oyy40v25umjcfxf2vsd 
       foreign key (ParameterValue_id) 
       references ParameterValue
create sequence hibernate_sequence start 1 increment 1

    create table bsnAttributes (
       Bundle_id int4 not null,
        symbolicNameAttributes varchar(255),
        symbolicNameAttributes_KEY varchar(255) not null,
        primary key (Bundle_id, symbolicNameAttributes_KEY)
    )

    create table Bundle (
       id  serial not null,
        description varchar(255),
        docUrl varchar(255),
        exportService varchar(32767),
        license varchar(8192),
        name varchar(255),
        symbolicName varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        exportPackage_id int8,
        importPackage_id int8,
        requireBundle_id int8,
        primary key (id)
    )

    create table MavenArtifact (
       id  serial not null,
        artifactId varchar(255) not null,
        groupId varchar(255) not null,
        version varchar(255) not null,
        primary key (id)
    )

    create table MavenResource (
       id  serial not null,
        MD5 varchar(255),
        SHA1 varchar(255),
        classifier varchar(255),
        fileExtension varchar(255),
        lastModified timestamp,
        packaging varchar(255),
        size int8,
        bundle_id int4,
        mavenArtifact_id int4 not null,
        primary key (id)
    )

    create table Package (
       DTYPE varchar(31) not null,
        id  serial not null,
        name varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        leftOpen boolean,
        rightMajor int4,
        rightMicro int4,
        rightMinor int4,
        rightQualifier varchar(255),
        rightOpen boolean,
        primary key (id)
    )

    create table Package_attrs (
       Package_id int4 not null,
        attrs varchar(32767),
        attrs_KEY varchar(255) not null,
        primary key (Package_id, attrs_KEY)
    )

    create table Parameters (
       id int8 not null,
        primary key (id)
    )

    create table Parameters_ParameterValue (
       Parameters_id int8 not null,
        params_id int8 not null,
        primary key (Parameters_id, params_id)
    )

    create table ParameterValue (
       id int8 not null,
        key varchar(8192) not null,
        primary key (id)
    )

    create table ParameterValue_attributes (
       ParameterValue_id int8 not null,
        attributes varchar(10485760),
        attributes_KEY varchar(255) not null,
        primary key (ParameterValue_id, attributes_KEY)
    )

    alter table MavenArtifact 
       add constraint UKknaqgfuee46wervds1ul0l0at unique (groupId, artifactId, version)
create index IDXimwxia9xlgxuw73yk80pi822x on MavenResource (classifier)
create index IDXiw2r993suw3hx0yupqrr2nkm6 on MavenResource (packaging)
create index IDX1vty24ygp7xcclr5jll0ty42u on MavenResource (MD5)
create index IDX15ub99oivyck7o1iwy8lmmodj on MavenResource (SHA1)

    alter table MavenResource 
       add constraint UKswf00u3a1p4wl3mbgsnv3bdxh unique (mavenArtifact_id, classifier)

    alter table Parameters_ParameterValue 
       add constraint UK_kahdvlkh7vhvx4ppvinxuniei unique (params_id)

    alter table bsnAttributes 
       add constraint FKqs7m5tnxxxu9pb4hyhk3u2igj 
       foreign key (Bundle_id) 
       references Bundle

    alter table Bundle 
       add constraint FKbcpvwvwuy6ypdw6a20dgptj46 
       foreign key (exportPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKb5t1o4dalplcch7c82dxtqkos 
       foreign key (importPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKgvg8dd5f89r51tpkhvpkop9c1 
       foreign key (requireBundle_id) 
       references Parameters

    alter table MavenResource 
       add constraint FK8reuvc1exmypamexrnanuhu9x 
       foreign key (bundle_id) 
       references Bundle

    alter table MavenResource 
       add constraint FKk1vhjcsypygcmsbxhec7i2ftm 
       foreign key (mavenArtifact_id) 
       references MavenArtifact

    alter table Package_attrs 
       add constraint FK1jky5aeeyiq336jp90t7hfgib 
       foreign key (Package_id) 
       references Package

    alter table Parameters_ParameterValue 
       add constraint FKli1sm0uctv6gd11581jfj9ecp 
       foreign key (params_id) 
       references ParameterValue

    alter table Parameters_ParameterValue 
       add constraint FKio2k2aord233kc7qgeam4vav6 
       foreign key (Parameters_id) 
       references Parameters

    alter table ParameterValue_attributes 
       add constraint FK8ptub2oyy40v25umjcfxf2vsd 
       foreign key (ParameterValue_id) 
       references ParameterValue
create sequence hibernate_sequence start 1 increment 1

    create table bsnAttributes (
       Bundle_id int4 not null,
        symbolicNameAttributes varchar(255),
        symbolicNameAttributes_KEY varchar(255) not null,
        primary key (Bundle_id, symbolicNameAttributes_KEY)
    )

    create table Bundle (
       id  serial not null,
        description varchar(255),
        docUrl varchar(255),
        exportService varchar(32767),
        license varchar(8192),
        name varchar(255),
        symbolicName varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        exportPackage_id int8,
        importPackage_id int8,
        requireBundle_id int8,
        primary key (id)
    )

    create table MavenArtifact (
       id  serial not null,
        artifactId varchar(255) not null,
        groupId varchar(255) not null,
        version varchar(255) not null,
        primary key (id)
    )

    create table MavenResource (
       id  serial not null,
        MD5 varchar(255),
        SHA1 varchar(255),
        classifier varchar(255),
        fileExtension varchar(255),
        lastModified timestamp,
        packaging varchar(255),
        size int8,
        bundle_id int4,
        mavenArtifact_id int4 not null,
        primary key (id)
    )

    create table Package (
       DTYPE varchar(31) not null,
        id  serial not null,
        name varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        leftOpen boolean,
        rightMajor int4,
        rightMicro int4,
        rightMinor int4,
        rightQualifier varchar(255),
        rightOpen boolean,
        primary key (id)
    )

    create table Package_attrs (
       Package_id int4 not null,
        attrs varchar(32767),
        attrs_KEY varchar(255) not null,
        primary key (Package_id, attrs_KEY)
    )

    create table Parameters (
       id int8 not null,
        primary key (id)
    )

    create table Parameters_ParameterValue (
       Parameters_id int8 not null,
        params_id int8 not null,
        primary key (Parameters_id, params_id)
    )

    create table ParameterValue (
       id int8 not null,
        key varchar(8192) not null,
        primary key (id)
    )

    create table ParameterValue_attributes (
       ParameterValue_id int8 not null,
        attributes varchar(10485760),
        attributes_KEY varchar(255) not null,
        primary key (ParameterValue_id, attributes_KEY)
    )

    alter table MavenArtifact 
       add constraint UKknaqgfuee46wervds1ul0l0at unique (groupId, artifactId, version)
create index IDXimwxia9xlgxuw73yk80pi822x on MavenResource (classifier)
create index IDXiw2r993suw3hx0yupqrr2nkm6 on MavenResource (packaging)
create index IDX1vty24ygp7xcclr5jll0ty42u on MavenResource (MD5)
create index IDX15ub99oivyck7o1iwy8lmmodj on MavenResource (SHA1)

    alter table MavenResource 
       add constraint UKswf00u3a1p4wl3mbgsnv3bdxh unique (mavenArtifact_id, classifier)

    alter table Parameters_ParameterValue 
       add constraint UK_kahdvlkh7vhvx4ppvinxuniei unique (params_id)

    alter table bsnAttributes 
       add constraint FKqs7m5tnxxxu9pb4hyhk3u2igj 
       foreign key (Bundle_id) 
       references Bundle

    alter table Bundle 
       add constraint FKbcpvwvwuy6ypdw6a20dgptj46 
       foreign key (exportPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKb5t1o4dalplcch7c82dxtqkos 
       foreign key (importPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKgvg8dd5f89r51tpkhvpkop9c1 
       foreign key (requireBundle_id) 
       references Parameters

    alter table MavenResource 
       add constraint FK8reuvc1exmypamexrnanuhu9x 
       foreign key (bundle_id) 
       references Bundle

    alter table MavenResource 
       add constraint FKk1vhjcsypygcmsbxhec7i2ftm 
       foreign key (mavenArtifact_id) 
       references MavenArtifact

    alter table Package_attrs 
       add constraint FK1jky5aeeyiq336jp90t7hfgib 
       foreign key (Package_id) 
       references Package

    alter table Parameters_ParameterValue 
       add constraint FKli1sm0uctv6gd11581jfj9ecp 
       foreign key (params_id) 
       references ParameterValue

    alter table Parameters_ParameterValue 
       add constraint FKio2k2aord233kc7qgeam4vav6 
       foreign key (Parameters_id) 
       references Parameters

    alter table ParameterValue_attributes 
       add constraint FK8ptub2oyy40v25umjcfxf2vsd 
       foreign key (ParameterValue_id) 
       references ParameterValue
create sequence hibernate_sequence start 1 increment 1

    create table bsnAttributes (
       Bundle_id int4 not null,
        symbolicNameAttributes varchar(255),
        symbolicNameAttributes_KEY varchar(255) not null,
        primary key (Bundle_id, symbolicNameAttributes_KEY)
    )

    create table Bundle (
       id  serial not null,
        description varchar(32767),
        docUrl varchar(255),
        exportService varchar(32767),
        license varchar(8192),
        name varchar(255),
        symbolicName varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        exportPackage_id int8,
        importPackage_id int8,
        requireBundle_id int8,
        primary key (id)
    )

    create table MavenArtifact (
       id  serial not null,
        artifactId varchar(255) not null,
        groupId varchar(255) not null,
        version varchar(255) not null,
        primary key (id)
    )

    create table MavenResource (
       id  serial not null,
        MD5 varchar(255),
        SHA1 varchar(255),
        classifier varchar(255),
        fileExtension varchar(255),
        lastModified timestamp,
        packaging varchar(255),
        size int8,
        bundle_id int4,
        mavenArtifact_id int4 not null,
        primary key (id)
    )

    create table Package (
       DTYPE varchar(31) not null,
        id  serial not null,
        name varchar(255) not null,
        major int4,
        micro int4,
        minor int4,
        qualifier varchar(8192),
        leftOpen boolean,
        rightMajor int4,
        rightMicro int4,
        rightMinor int4,
        rightQualifier varchar(255),
        rightOpen boolean,
        primary key (id)
    )

    create table Package_attrs (
       Package_id int4 not null,
        attrs varchar(32767),
        attrs_KEY varchar(255) not null,
        primary key (Package_id, attrs_KEY)
    )

    create table Parameters (
       id int8 not null,
        primary key (id)
    )

    create table Parameters_ParameterValue (
       Parameters_id int8 not null,
        params_id int8 not null,
        primary key (Parameters_id, params_id)
    )

    create table ParameterValue (
       id int8 not null,
        key varchar(8192) not null,
        primary key (id)
    )

    create table ParameterValue_attributes (
       ParameterValue_id int8 not null,
        attributes varchar(10485760),
        attributes_KEY varchar(255) not null,
        primary key (ParameterValue_id, attributes_KEY)
    )

    alter table MavenArtifact 
       add constraint UKknaqgfuee46wervds1ul0l0at unique (groupId, artifactId, version)
create index IDXimwxia9xlgxuw73yk80pi822x on MavenResource (classifier)
create index IDXiw2r993suw3hx0yupqrr2nkm6 on MavenResource (packaging)
create index IDX1vty24ygp7xcclr5jll0ty42u on MavenResource (MD5)
create index IDX15ub99oivyck7o1iwy8lmmodj on MavenResource (SHA1)

    alter table MavenResource 
       add constraint UKswf00u3a1p4wl3mbgsnv3bdxh unique (mavenArtifact_id, classifier)

    alter table Parameters_ParameterValue 
       add constraint UK_kahdvlkh7vhvx4ppvinxuniei unique (params_id)

    alter table bsnAttributes 
       add constraint FKqs7m5tnxxxu9pb4hyhk3u2igj 
       foreign key (Bundle_id) 
       references Bundle

    alter table Bundle 
       add constraint FKbcpvwvwuy6ypdw6a20dgptj46 
       foreign key (exportPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKb5t1o4dalplcch7c82dxtqkos 
       foreign key (importPackage_id) 
       references Parameters

    alter table Bundle 
       add constraint FKgvg8dd5f89r51tpkhvpkop9c1 
       foreign key (requireBundle_id) 
       references Parameters

    alter table MavenResource 
       add constraint FK8reuvc1exmypamexrnanuhu9x 
       foreign key (bundle_id) 
       references Bundle

    alter table MavenResource 
       add constraint FKk1vhjcsypygcmsbxhec7i2ftm 
       foreign key (mavenArtifact_id) 
       references MavenArtifact

    alter table Package_attrs 
       add constraint FK1jky5aeeyiq336jp90t7hfgib 
       foreign key (Package_id) 
       references Package

    alter table Parameters_ParameterValue 
       add constraint FKli1sm0uctv6gd11581jfj9ecp 
       foreign key (params_id) 
       references ParameterValue

    alter table Parameters_ParameterValue 
       add constraint FKio2k2aord233kc7qgeam4vav6 
       foreign key (Parameters_id) 
       references Parameters

    alter table ParameterValue_attributes 
       add constraint FK8ptub2oyy40v25umjcfxf2vsd 
       foreign key (ParameterValue_id) 
       references ParameterValue
