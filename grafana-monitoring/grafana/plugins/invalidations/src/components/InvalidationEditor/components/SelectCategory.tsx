import React from 'react';
import { Field, Input } from '@grafana/ui';
import { FormContext } from '../Editor';
import { CATEGORYOPTIONS } from '../options';

export function SelectCategory({ handleChange, valueLabel }: any) {
  const { errors, control }: any = React.useContext(FormContext);
  const value = CATEGORYOPTIONS.find(o => o.value === valueLabel);
  control.setValue('category', value);

  return (
    <Field label="Category" invalid={!!errors.category} error="Category is required">
      {/* <InputControl
        /* Render InputControl as controlled input (Select) */
        // as={Select}
        /* Pass control exposed from Form render prop */
        //   control={control}
        //   name="category"
        //   defaultValue={value}
        //   options={CATEGORYOPTIONS}
        //   rules={{ required: true }}
        //    onChange={([v]: any) => handleChange(v)}
        // /> */}
        /* In case of Select the value has to be returned as an object with a `value` key for the value to be saved to form data */

        <Input
          name={`toto`}
          defaultValue={value as any}
        />
      }</Field>
  );
}
