import { StandardEditorProps } from '@grafana/data';
import {
  Button,
  Collapse,
  Field,
  HorizontalGroup,
  Input,
  InputControl,
  Label,
  Select,
  stylesFactory,
  VerticalGroup,
} from '@grafana/ui';
import React, { useState } from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { css, cx } from 'emotion';
import { DEFAULTVALUEROOTCAUSE, SELECTOPTIONSICONS } from './options';
import { FormDTO, RootCauseEditorOptions, RootCauseEditorSettings } from './types';

interface Props extends StandardEditorProps<RootCauseEditorOptions[], RootCauseEditorSettings, any> {}

const RootCauseEditor: React.FC<Props> = ({ value, onChange, item }) => {
  const styles = getStyles();
  const [isOpen, setIsOpen] = useState(false); //Collapse
  const impact = item.settings?.impact ? item.settings?.impact : '';
  const { register, control, handleSubmit } = useForm<any, FormDTO>({
    mode: 'onChange',
    defaultValues: {
      rootCauses: value,
    },
  });
  //TODO we don't use the grafana's Field Array because it's not working with the 7.2 version, make the change in case of grafana update
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'rootCauses',
  });
  const onSubmit = (formData: FormDTO): void => {
    if (formData.rootCauses) {
      onChange(formData.rootCauses);
    } else {
      formData.rootCauses = DEFAULTVALUEROOTCAUSE;
      onChange(formData.rootCauses);
    }
  };

  return (
    /*TODO we don't use the grafanaUI's FORM because it's not working with the 7.2 version, make the change in case of
          grafana update*/
    <div className={cx(styles.container)}>
      <Collapse collapsible label={impact} isOpen={isOpen} onToggle={() => setIsOpen(!isOpen)}>
        <form onSubmit={handleSubmit(onSubmit)}>
          <VerticalGroup>
            {fields.map((item, index) => {
              return (
                <VerticalGroup key={item.id}>
                  <Label className={cx(styles.label)} description="Displayed name of invalidation in toolbox">
                    Label
                  </Label>
                  <Input required name={`rootCauses[${index}].label`} defaultValue={`${item.label}`} ref={register()} />

                  <Label className={cx(styles.label)} description="Short description of the invalidation">
                    Description
                  </Label>
                  <Input
                    name={`rootCauses[${index}].description`}
                    defaultValue={`${item.description}`}
                    ref={register()}
                  />

                  <Label className={cx(styles.label)} description="How invalidation will be stored in the datatabase">
                    Value
                  </Label>
                  <Input required name={`rootCauses[${index}].value`} defaultValue={`${item.value}`} ref={register()} />

                  <Label className={cx(styles.label)} description="Icon of the invalidation in toolbox">
                    Icon
                  </Label>
                  <Field required>
                    <InputControl
                      name={`rootCauses[${index}].imgUrl`}
                      control={control}
                      rules={{
                        required: false,
                      }}
                      defaultValue={`${item.imgUrl}`}
                      onChange={([selected]) => {
                        return selected.value;
                      }}
                      options={SELECTOPTIONSICONS}
                      as={Select}
                    />
                  </Field>
                  <Button
                    className={cx(styles.ButtonTrash)}
                    variant="destructive"
                    icon="trash-alt"
                    name="Delete"
                    size="md"
                    onClick={() => {
                      remove(index);
                    }}
                  ></Button>
                </VerticalGroup>
              );
            })}
          </VerticalGroup>
          <VerticalGroup>
            <HorizontalGroup className={cx(styles.label)}>
              <Button
                className={cx(styles.ButtonAppend)}
                type="button"
                onClick={() => {
                  append({
                    label: '',
                    description: '',
                    value: '',
                    imgUrl: '',
                  });
                }}
              >
                append
              </Button>
              <Button className={cx(styles.ButtonValidate)} type="submit">
                validate
              </Button>
            </HorizontalGroup>
          </VerticalGroup>
        </form>
      </Collapse>
    </div>
  );
};
export default RootCauseEditor;

const getStyles = stylesFactory(() => {
  return {
    container: css`
      margin-top: -20px;
    `,
    label: css`
      margin-bottom: -5px;
    `,
    ButtonTrash: css`
      margin-bottom: 20px;
    `,
    ButtonAppend: css`
      margin-right: 15px;
      margin-bottom: 10px;
    `,
    ButtonValidate: css`
      margin-bottom: 10px;
    `,
  };
});
