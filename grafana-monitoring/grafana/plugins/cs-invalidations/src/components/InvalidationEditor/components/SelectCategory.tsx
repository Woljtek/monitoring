import React from 'react';
import { Field, Select, InputControl } from '@grafana/ui';
import { FormContext } from '../Editor';
import { CATEGORYOPTIONS } from '../options';


export function SelectCategory({
  handleChange,
  valueLabel,
}: {
  handleChange: (values: any[]) => void;
  valueLabel: string;
}) {
  const { errors, control }: any = React.useContext(FormContext);
  const value = CATEGORYOPTIONS.find((o: any) => o.value === valueLabel);
  control.register('responsibility', value?.value);

  return (
    <Field label="Responsibility" invalid={!!errors.category} error="Responsibility is required">

      <InputControl
        name="responsibility"
        control={control}
        rules={{
          required: false,
        }}
        // defaultValue={value}
        render={({ field }) => <Select {...field} options={CATEGORYOPTIONS} value={value} onChange={([v]: any) => { handleChange(v.value) }} />}


      />

    </Field>
  );
}
