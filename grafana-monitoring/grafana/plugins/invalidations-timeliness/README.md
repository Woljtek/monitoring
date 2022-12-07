## Description

This plugin is a panel used to manage invalidations associated to product.

It display a table with selectable rows and action buttons above the table:

 - `create` an invalidation associated to the product. This action is
   available when all selected products are not linked to an existing
   invalidation. It opens a drawer with a form to fill to create the
   invalidation.

 - `edit` the invalidation associated to the selected product. This action is
   available when only one product is selected. It opens a drawer with a form
   to fill to edit the invalidation. If the invalidation is also linked to
   other products, it will ask if you want to modify the invalidation and
   thus affect all products or create a new invalidation for the selected
   product.

 - `link` an invalidation to the selected products. This action is available
   when all selected products are not already linked to an invalidation. It
   opens a drawer with a select button to select an existing invalidation id.
   You can pick a time range to change the select options or directly type an
   id in the select. If the id exists you will be able to save.

 - `unlink` the invalidation associated to the selected product. This action is
   available when only one product linked to an invalidation is
   selected.

 - `delete` the invalidation associated to the selected products. This action
   is available when all selected products are linked to an invalidation (it
   can be different invalidation for each product). It will ask confirmation.
   Before actually delete the invalidation from the database, it will unlink
   all products linked to the invalidation.

### Configuration

<!-- - You must provide a datasource in the display section of options in order to
perform actions. -->

The plugin assumes that the panel is associated to a `PostgreSQL` datasource
with the following query:

```sql
  SELECT DISTINCT product.id,invalidation.root_cause AS root_cause,invalidation.id AS inval_id,responsibility,comment,label,anomaly_identifier
  FROM product 
  LEFT JOIN invalidation_timeliness AS it ON product.id = ANY(it.product_ids)
  LEFT JOIN invalidation ON invalidation.id = parent_id
  LEFT JOIN output_list ol ON ol.product_id = product.id 
  LEFT JOIN processing proc ON proc.id  = ol.processing_id 
  WHERE NOT proc.duplicate
```
- You can configure the list of root cause
You can add a root cause (append Button) or delete a root cause (trash icon button)
Click on validate button to save the new list

------
