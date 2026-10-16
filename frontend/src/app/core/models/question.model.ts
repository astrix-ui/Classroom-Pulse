export interface QuestionDto {
  questionId: string;
  questionText: string;
  sequenceNumber: number;
  publishedAt: string;
}

export interface PublishQuestionRequest {
  questionText: string;
}

export interface CompleteQuestionRequest {
  questionId: string;
}

