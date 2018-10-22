# Colleague DMI Client Sample

This is a sample of using the `DmiEntityService`.

> NOTE: To run this class in IntelliJ you'll need to uncomment the excludes portion of maven-compiler-plugin

## Setup

To run this program you'll need to create a file called `dmi.credentials` with the following fields:

    username=(username)
    password=(password)
    account=dev0_rt
    host=host.name
    port=9521
    secure=false
    hostname.override=
    shared.secret=(shared secret)

## What this sample program does

This example is a thorough usage of `DmiEntityService`. This example also shows the benefits of caching and optionally
enabling concurrent joins.

See each file in the `model` package for examples of using various usages of fields, joins and associations. 

Tips:

1. Entity classes must be annotated with `@Entity` and extend `ColleagueRecord`
2. Association classes should be annotated with `@AssocationEntity`
3. Fields and joins will be mapped by default by name, converting camel case to Colleague field names (ie lastName is mapped to LAST.NAME)
4. Field name mapping can be overridden using the `@Field` annotation
5. Joins must be annotated with `@Join`. The field name must correspond to the name of the pointer, or the optional value
   attribute of `@Join` can be used to specify the join.
6. Data types must correspond to what is returned by `DmiDataService` (see the main README for data types)

See StudentAcadCredRecord and child classes for a thorough example of each use case. 
   
   