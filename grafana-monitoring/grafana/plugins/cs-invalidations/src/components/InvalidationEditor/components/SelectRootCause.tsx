import React from 'react';
import { Field, Select, InputControl } from '@grafana/ui';
import { FormContext, SelectContext } from '../Editor';

export function SelectRootCause({
  handleChange,
  defaultValue,
}: {
  handleChange: (values: any[]) => void;
  defaultValue: string;
}) {
  const { errors, control }: any = React.useContext(FormContext);
  const { rootCause } = React.useContext(SelectContext);
  const value = defaultValue === null ? rootCause.value : defaultValue; // undefined is a valid value
  const optionValue = rootCause.options.find(o => o.value === value)
    ? rootCause.options.find(o => o.value === value)
    : '';
  control.register('rootCause', optionValue);

  return (
    <Field label="Root Cause" invalid={!!errors.rootCause} error="Root cause is required">

      <InputControl
        name="select"

        control={control}
        rules={{ required: true }}

        render={({ field }) => <Select {...field} disabled={rootCause.disabled} options={rootCause.options} value={optionValue} onChange={([v]: any) => handleChange(v.value)} />}


      />
    </Field>
  );
}
