import React from 'react';
import { Field, Input } from '@grafana/ui';
import { FormContext, SelectContext } from '../Editor';

export function SelectRootCause({ defaultValue }: { defaultValue: string }) {
  const { errors, control }: any = React.useContext(FormContext);
  const { rootCause } = React.useContext(SelectContext);
  const value = defaultValue === null ? rootCause.value : defaultValue; // undefined is a valid value
  const optionValue = rootCause.value === '' ? null : rootCause.options.find((o: any) => o.value === value);
  control.setValue('rootCause', optionValue);

  return (
    <Field label="Root Cause" invalid={!!errors.rootCause} error="Root cause is required">
      <Input
        /* Render InputControl as controlled input (Select) */

        defaultValue={optionValue as any}

      />
    </Field>
  );
}
