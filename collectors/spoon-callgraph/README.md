# How to run:

On terminal run using maven:

`mvn compile exec:java`

When prompted input the clone link or local project folder path of the repository
you want to analyse and the ORM Technology 
that is used in that repository.

To match data classes on custom regexes in its FQN, you can input a list of regexes
seperated by a comma. 

You can also match classes belonging to microservices on a custom regex in its path, which
is necessery for the generation of the microservice decomposition.

After running, a folder ```data/collection/PROGRAM_NAME``` will be created. This contains
a ```functionalities.json``` file with the sequence of calls of the program,
in combination with a ```decomposition.json``` file with the microservice decomposition 
of the program. Both of these files are ready to be given as inputs to the backend. 
As extra, an ```entities.json``` file with a list of all domain entities is created as well.

#### Assumptions:
* A SpringDataJPA projects only access the database via SpringData Repositories, i.e.,
there are no references to EntityManager or SessionFactory.

* @Query annotations do not explicitly reference namedQueries.
This does not mean that the project can not have @NamedQuery(ies)
or @NamedNativeQuery(ies);

* We assume that interfaces have at most one explicit implementation
and we redirect calls to interfaces to the respective interface implementor 

* Inheritance Strategy (SINGLE_TABLE) has no more than one level of inheritance

* Assuming no fully qualified path names inside HQL 'From' clauses

* Assuming no comments on database access queries

* Assuming non-sensitive naming
