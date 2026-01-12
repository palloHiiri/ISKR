export interface CVPStatus {
  cvpId: number;
  collectionId: number;
  userId: number;
  cvpStatus: 'Allowed' | 'Denied';
  user?: {
    userId: number;
    username: string;
    nickname: string;
    email: string;
  };
}

export interface AddCVPData {
  cvpStatus: 'Allowed' | 'Denied';
  userId: number;
}

export interface UserSearchResult {
  id: number;
  username: string;
  nickname: string;
  email: string;
  subscribersCount: number;
  imageUuid?: string;
  imageExtension?: string;
}