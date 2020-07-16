# Schema Definition Parser

## Introduction
The purpose of the schema definition parser is to read XML files containing the domain specific schema model.
The XML can be used to create new types or mixins and to create new attributes within types or mixins.
It is not possible to remove attributes from a schema model.

The XML can be split into multiple files. A single root file may be used to define a core schema model for a domain.
Within extension XML files this core is extended with further types, mixins or attributes.

### Schema Meta Model

#### Type and Mixin
Both _types_ and _mixins_ consist of attributes and can be in relation to each other.
The core difference is, that mixins can not be instantiated directly. Their instances only exist, if the mixin is mixed
into a type and a new instance of that type is being created. A further difference is, that types can make use of
inheritance, while mixins do not offer such a mechanism.

#### Attribute
Attributes are used to define a place for information within a type or mixin. Attributes can be simple values or
references to other type instances.

#### Virtual
Types, mixins and attributes can be declared as _virtual_. This has different impact on their behavior.

| Definition | non-virtual | virtual |
| --- | --- | --- |
| Type | Instances can be created. They will be stored in the database and they are queryable.  | Instances can be created, but they are not persisted individually. Instances may be stored as JSON in the database but they may also be transient instances resolved by an adapter. |
| Mixin | Instances can only be created via a type, that is mixed with this mixin. Non-virtual mixins can be queried in the database. The specific type(s) do not need to be known in order to define a query. | Instances can be created, but they are only stored as JSON or transient instances provided by an adapter. It is important to note, that a virtual mixin may have non-virtual attributes. This means, that a non-virtual type can be queried on non-virtual attributes defined in a virtual mixin. |
| Attribute | The attribute is stored in the database and is most likely available in queries. | The attribute is not stored in the database but rather provided by an adapter. The adapter may perform complex logic in order to determine the attributes value. |

### XML information
As already mentioned the schema is defined via XML. The following code shows a short example:

```xml
<?xml version="1.0" ?>
<schema namespace="http://your.domain/1.0" name="mydomain">
    <!-- a simple mixin definition -->
    <DriverMixin kind="mixin">
        <license kind="string" />
    </DriverMixin>
    <!-- a simple type definition -->
    <Person kind="type">
        <name kind="string" />
    </Person>
    <!-- mixing a mixin into a type -->
    <RaceDriver kind="type" mixWith="DriverMixin">
        <league kind="string" />
    </RaceDriver>
    <CasualDriver kind="type" mixWith="DriverMixin" />
    <!-- an abstract type definition -->
    <AbstractDriveable kind="type" abstract="true">
        <!-- reference an other entity via a mixin name -->
        <driver kind="mixin" mixin="Driver" />
    </AbstractDriveable>
    <!-- extending an abstract type -->
    <Car kind="type" extend="AbstractDriveable">
        <!-- make a list of entities available as a first level attribute -->
        <seats kind="inverseType" type="CarSeat" attribute="car" cascadeDelete="true" />
        <!-- embed an other type instance as JSON in the current type table -->
        <details kind="jsonType" type="TechnicalDetails" />
        <!-- the value for a virtual attribute needs be provided by an adapter -->
        <currentSpeed kind="decimal" virtual="true" />
    </Car>
    <CarSeat kind="type">
        <!-- reference a mandatory other entity instance -->
        <car kind="type" type="Car" mandatory="true"/>
    </CarSeat>
    <!-- a virtual type, that will not be persisted in individual tables -->
    <TechnicalDetails kind="type" virtual="true">
        <maxSpeed kind="decimal" />
    </TechnicalDetails>
</schema>
```

The following conventions need to be respected:
1. Type and mixin names always start with a capital letter and are written in camel-case.
2. Attribute names always start with a lowercase letter and are written in camel-case.

#### Defining a type or mixin
In order to define a type or mixin create an XML-element with the name of the type or mixin. In order to distinguish type and mixin definitions add the `kind` attribute with either `type` or `mixin` as the value.

```xml
<DriverMixin kind="mixin" />
<Person kind="type"/>
```

#### Abstract types and using mixins
In order to make a type abstract the attribute `abstract` needs to be set to `true` on the type definition:

```xml
<AbstractDriveable kind="type" abstract="true" />
```

A type can extend an other type by adding the `extend` attribute to the type definition. The attribute value points to the name of that type, that shall be extended.

```xml
<Car kind="type" extend="AbstractDriveable" />
```

A mixin can be mixed into a type by using the `mixWith` attribute. Here multiple mixins can be referenced as a comma separated list of mixin names without any whitespaces.

```xml
<CasualDriver kind="type" mixWith="DriverMixin" />
```

#### Defining virtual types or mixins
A type or mixin becomes virtual, when the attribute `virtual` is set to `true` on the according definition.

```xml
<TechnicalDetails kind="type" virtual="true" />
```

#### Defining attributes
Attributes are defined via XML-elements within a type or mixin element. Each attribute needs to have the attribute `kind`, that specifies which kind of attribute is being defined. Available kinds are:

- `string`: textual attribute. parameters:
    - `length` the number of bytes to use for storing the value in the DB. default: `255`
    - `isLong` a boolean marker for marking strings as long text, that will not be used in queries. default: `false`
- `decimal` number attribute. parameters:
    - `decimalPlaces` the number of decimal places to reserve for the decimal number. default: `null`
    - `length` the maximum number of digits to accept for the decimal number. default: `null`
    - examples:
        - `length=null && (decimalPlaces=null || decimalPlaces=0)` will create a `Long`
        - `length=null && decimalPlaces>0` will create a `Double`
        - `length!=null` will create a `BigDecimal`
- `boolean` boolean attribute. no parameters
- `date` date attribute with date and time information. no parameters
- `binary` blob attribute. parameters:
    - `asByteArray` load the blob as a fixed array of bytes. If set to `false`, the blob will be loaded as an `InputStream`. default: `false`
- `jsonType` embedded entity, that is defined via a type name. parameters:
    - `type` the name of the referenced type
- `jsonMixin` embedded entity, that is defined via a mixin name. parameters:
    - `mixin` the name of the referenced mixin
- `type` reference to a single entity via its type. parameters:
    - `type` the name of the referenced type
    - `cascadeDelete` delete the current entity, when the referenced entity is deleted. default: `false`
    - `nullOnDelete` set the reference to `null`, when the referenced entity is deleted. default: `false`
    - `deleteOrphans` delete the referenced entity, when the current entity is deleted. default: `false`
    - `toOneAttribute` the name of the attribute in the referenced entity, that is used to express a 1-1 relation. default: `null`
- `mixin` reference to a single entity via its mixin. parameters:
    - `mixin` the name of the referenced mixin
    - `cascadeDelete` delete the current entity, when the referenced entity is deleted. default: `false`
    - `nullOnDelete` set the reference to `null`, when the referenced entity is deleted. default: `false`
    - `deleteOrphans` delete the referenced entity, when the current entity is deleted. default: `false`
- `inverseType` list of entities, that are referenced via their type name and point to the current type or mixin. parameters:
    - `type` the name of the type of the list items
    - `attribute` the name of the attribute, that points to the current type or mixin
    - `deleteOrphans` delete the entities, that point to the current type or mixin, when the current entity is deleted.
- `inverseMixin` list of entities, that are referenced via their mixin name and point to the current type or mixin. parameters:
    - `mixin` the name of the mixin of the list items
    - `attribute` the name of the attribute, that points to the current type or mixin
    - `deleteOrphans` delete the entities, that point to the current type or mixin, when the current entity is deleted.

### Patterns

#### 1-N relation
A 1-N relation is expressed with at least one `type` or `mixin` attribute. There is not need to add the according `inverseType` or `inverseMixin` attribute the schema. The inverse attribute is simply adding the query for referencing entities to the default set of attributes, because they might be accessed frequently.

#### M-N relation
A M-N relation can not be expressed directly in the schema. Instead the M-N relation needs to be split into a 1-M and a 1-N relation by introducing an associate type.

Here is an example to express the M-N relation of cars and persons.

```xml
<Car kind="type">
    <passengers kind="inverseType" type="Passenger" attribute="car" />
</Car>
<Person kind="type">
    <name kind="string" />
</Person>
<Passenger kind="type">
    <car kind="type" type="Car" mandatory="true" />
    <person kind="type" type="Person" mandatory="true" />
</Passenger>
```

#### 1-1 relation
Using 1-1 relations should generally be avoided, because the information could be stored in a single entity.

Lets assume a scenario where an access management is defining grants via a `Clearance` entity. The `Clearance` can only be used once. When the `Clearance` is deleted, the usage shall be deleted as well.

```xml
<Clearance kind="type">
    <grantedOn kind="date" />
    <usage kind="type" type="Usage" />
</Clearance>
<Usage kind="type">
    <clearance kind="type" type="Clearance" toOneAttribute="usage" cascadeDelete="true" mandatory="true" />
</Usage>
```
