# Schema Impl

## Introduction

This bundle is the default implementation of the _Schema API_.

## Listeners

You can attach listeners to a schema engine by either manually calling the `addListener` and `removeListener` methods on the `org.bndly.schema.api.services.Engine` object or by simply registering an OSGI service with at least a `schema` property, that contains the name of the schema engine to which it should be attached.

Here is an example for the OSGI service approach, that shows how to implement a listener, that will be automatically registered for the schema `yourschemaname`.

```
@Component
@Service(PersistListener.class)
@Properties({
	@Property(name="schema", value="yourschemaname")
})
public class MyListener implements PersistListener {
	... your listener implementation will be here
}
```