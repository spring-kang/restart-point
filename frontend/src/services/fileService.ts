import api from './api';

export const fileService = {
  /**
   * 파일 업로드
   * @param file 업로드할 파일
   * @param directory 저장 디렉토리 (기본값: general)
   * @returns 업로드된 파일 URL
   */
  async uploadFile(file: File, directory: string = 'general'): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('directory', directory);

    const response = await api.post<{ url: string }>('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data.url;
  },
};
