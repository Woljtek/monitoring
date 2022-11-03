import React from 'react';
import { Field, TextArea } from '@grafana/ui';
import { FormContext } from '../Editor';

export function Comment({ defaultValue }: { defaultValue: string }) {
  const { register }: any = React.useContext(FormContext);
  const value = defaultValue || undefined;
  return (
    <Field label="Comment">
      <TextArea name="comment" ref={register} placeholder="Free comment" contentEditable draggable>
        {value}
      </TextArea>
    </Field>
  );
}
