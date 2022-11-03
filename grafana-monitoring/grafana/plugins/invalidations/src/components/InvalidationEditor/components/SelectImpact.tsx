import React from 'react';
import { Field, Input } from '@grafana/ui';
import { FormContext, SelectContext } from '../Editor';

export function SelectImpact({ defaultValue }: { defaultValue: string }) {
  const { errors, control }: any = React.useContext(FormContext);
  const { impact } = React.useContext(SelectContext);
  const value = defaultValue ?? impact.value;
  const optionValue = impact.options.find(o => o.value === value);
  control.setValue('impact', optionValue);

  return (
    <Field label="Impact" invalid={!!errors.impact} error="Select is required" disabled={impact.disabled}>
      <Input
        /* Render InputControl as controlled input (Select) */

        defaultValue={optionValue as any}

      />
    </Field>
  );
}
