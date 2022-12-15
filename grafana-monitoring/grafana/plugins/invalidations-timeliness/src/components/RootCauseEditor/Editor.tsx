import { StandardEditorProps } from '@grafana/data';
import {
  Button,
  HorizontalGroup,
  Input,
  InputControl,
  Label,
  Select,
  stylesFactory,
  VerticalGroup,
} from '@grafana/ui';
import React from 'react';
import { useForm, useFieldArray } from 'react-hook-form';
import { css, cx } from 'emotion';
import { DEFAULTVALUEROOTCAUSE, SELECTOPTIONSICONS } from './options';
import { FormDTO, RootCauseEditorOptions, RootCauseEditorSettings } from './types';

interface Props extends StandardEditorProps<RootCauseEditorOptions[], RootCauseEditorSettings, any> { }

const RootCauseEditor: React.FC<Props> = ({ value, onChange, item }) => {
  const styles = getStyles();
  const { control, handleSubmit, register } = useForm<any, FormDTO>({
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
      <form onSubmit={handleSubmit(onSubmit)}>
        <VerticalGroup>
          {fields && fields.map((item: any, index: number) => {
            return (
              <div key={item.id}>
                <VerticalGroup >
                  <Label className={cx(styles.label)} description="Displayed name of invalidation in toolbox">
                    Label
                  </Label>
                  <Input
                    required
                    defaultValue={item.label}
                    {...register(`rootCauses[${index}].label`)}
                  />
                  <Label className={cx(styles.label)} description="Short description of the invalidation">
                    Description
                  </Label>
                  <Input
                    defaultValue={item.description}
                    {...register(`rootCauses[${index}].description`)}
                  />
                  <Label className={cx(styles.label)} description="How invalidation will be stored in the datatabase">
                    Value
                  </Label>
                  <Input required defaultValue={item.value} {...register(`rootCauses[${index}].value`)} />
                  <Label className={cx(styles.label)} description="Icon of the invalidation in toolbox">
                    Icon
                  </Label>
                  <InputControl
                    name={`rootCauses[${index}].imgUrl`}
                    control={control}
                    rules={{
                      required: false,
                    }}
                    defaultValue={item.imgUrl}
                    render={({ field }) =>
                      <Select
                        {...field}
                        options={SELECTOPTIONSICONS}
                        onChange={(e) => field.onChange(e.imgUrl)}
                      />}
                  />
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
              </div>
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
