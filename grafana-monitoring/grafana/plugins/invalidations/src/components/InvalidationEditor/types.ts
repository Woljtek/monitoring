import { SelectableValue } from '@grafana/data';

export interface FormDTO {
  category: SelectableValue<string>;
  impact: SelectableValue<string>;
  rootCause: SelectableValue<string>;
  cams: SelectableValue<number>;
  comment: string;
}

export interface SelectOptionsInterface {
  [key: string]: {
    [key: string]: {
      disabled: boolean;
      value: string | undefined;
      options: Array<SelectableValue<string>>;
    };
  };
}

export interface SelectCategory {
  [key: string]: {
    disabled: boolean;
    value: string | undefined;
    options: Array<SelectableValue<string>>;
  };
}
