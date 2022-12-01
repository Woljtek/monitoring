export interface RootCauseEditorOptions {
  value: string | undefined;
  description: string;
  imgUrl: string;
  label: string;
}

export interface RootCauseEditorSettings {
  impact: string;
}

export interface FormDTO {
  rootCauses: RootCauseEditorOptions[];
}
