import { useState, useEffect } from 'react';
import './CollectionListModal.scss';
import CardElement from "../card-element/CardElement.tsx";
import PrimaryButton from "../primary-button/PrimaryButton.tsx";
import { useSelector } from 'react-redux';
import type { RootState } from '../../../redux/store.ts';
import collectionAPI, { type BookInCollectionStatus } from '../../../api/collectionService.ts';
import { getImageUrl } from '../../../api/popularService.ts';
import PlaceholderImage from '../../../assets/images/placeholder.jpg';

interface CollectionListModalProps {
  bookId?: number;
  onCollectionSelected?: (collectionIds: number[]) => void;
  onClose?: () => void;
}

function CollectionListModal({ bookId, onCollectionSelected, onClose }: CollectionListModalProps) {
  const [collections, setCollections] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCollections, setSelectedCollections] = useState<Set<number>>(new Set());
  const [processingCollections, setProcessingCollections] = useState<Set<number>>(new Set());
  const [bookInCollectionStatus, setBookInCollectionStatus] = useState<Record<number, boolean>>({});

  const currentUser = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);

  useEffect(() => {
    if (isAuthenticated) {
      loadCollections();
    }
  }, [isAuthenticated]);

  useEffect(() => {
    if (bookId && collections.length > 0) {
      checkBookInAllCollections();
    }
  }, [bookId, collections]);

  const loadCollections = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await collectionAPI.getUserCollections();
      const filteredCollections = response.collections;
      setCollections(filteredCollections);
    } catch (err: any) {
      console.error('Error loading collections:', err);
      setError(err.message || 'Ошибка загрузки коллекций');
    } finally {
      setLoading(false);
    }
  };

  const checkBookInAllCollections = async () => {
    if (!bookId) return;

    try {
      const statusMap: Record<number, boolean> = {};
      const selected = new Set<number>();

      // Проверяем для каждой коллекции наличие книги
      for (const collection of collections) {
        try {
          const status = await collectionAPI.checkBookInCollection(collection.collectionId, bookId);
          statusMap[collection.collectionId] = status.exists;
          if (status.exists) {
            selected.add(collection.collectionId);
          }
        } catch (err) {
          console.error(`Error checking book in collection ${collection.collectionId}:`, err);
          statusMap[collection.collectionId] = false;
        }
      }

      setBookInCollectionStatus(statusMap);
      setSelectedCollections(selected);
    } catch (err: any) {
      console.error('Error checking book in collections:', err);
    }
  };

  const handleCollectionClick = async (collectionId: number) => {
    if (!bookId) {
      // Если нет bookId, просто переключаем выбор
      const newSelected = new Set(selectedCollections);
      if (newSelected.has(collectionId)) {
        newSelected.delete(collectionId);
      } else {
        newSelected.add(collectionId);
      }
      setSelectedCollections(newSelected);
      return;
    }

    // Если есть bookId, добавляем/удаляем книгу из коллекции
    try {
      setProcessingCollections(prev => new Set(prev).add(collectionId));

      const isInCollection = selectedCollections.has(collectionId);

      if (isInCollection) {
        // Удаляем книгу из коллекции
        await collectionAPI.removeBookFromCollection(collectionId, bookId);
        const newSelected = new Set(selectedCollections);
        newSelected.delete(collectionId);
        setSelectedCollections(newSelected);
        setBookInCollectionStatus(prev => ({
          ...prev,
          [collectionId]: false
        }));
      } else {
        // Добавляем книгу в коллекцию
        await collectionAPI.addBookToCollection(collectionId, bookId);
        const newSelected = new Set(selectedCollections);
        newSelected.add(collectionId);
        setSelectedCollections(newSelected);
        setBookInCollectionStatus(prev => ({
          ...prev,
          [collectionId]: true
        }));
      }
    } catch (err: any) {
      console.error('Error toggling book in collection:', err);
      setError(err.message || 'Ошибка при обновлении коллекции');
    } finally {
      setProcessingCollections(prev => {
        const newSet = new Set(prev);
        newSet.delete(collectionId);
        return newSet;
      });
    }
  };

  const handleConfirm = () => {
    if (onCollectionSelected) {
      onCollectionSelected(Array.from(selectedCollections));
    }
    if (onClose) {
      onClose();
    }
  };

  const getImageUrlForCollection = (collection: any): string => {
    if (collection.photoLink) {
      return getImageUrl(collection.photoLink) || PlaceholderImage;
    }
    return PlaceholderImage;
  };

  const getConfidentialityText = (confidentiality: string): string => {
    return confidentiality === 'Public' ? 'Публичная' : 'Приватная';
  };

  const getButtonLabel = (isInCollection: boolean): string => {
    return isInCollection ? 'Убрать из коллекции' : 'Добавить в коллекцию';
  };

  if (loading) {
    return (
      <div
        className="collection-list-modal-container"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="collection-list-modal-title">
          Загрузка коллекций...
        </h2>
        <div className="loading-message">
          Пожалуйста, подождите
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div
        className="collection-list-modal-container"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="collection-list-modal-title">
          Ошибка
        </h2>
        <div className="error-message">
          {error}
        </div>
        <div className="modal-actions">
          <PrimaryButton
            label="Закрыть"
            onClick={() => onClose && onClose()}
            type="button"
          />
        </div>
      </div>
    );
  }

  return (
    <div
      className="collection-list-modal-container"
      onClick={(e) => e.stopPropagation()}
    >
      <h2 className="collection-list-modal-title">
        {bookId ? 'Добавить книгу в коллекции' : 'Выберите коллекции'}
      </h2>

      {collections.length === 0 ? (
        <div className="no-collections-message">
          У вас пока нет коллекций. Создайте первую коллекцию в разделе "Мои коллекции".
        </div>
      ) : (
        <>
          <div className="collection-list">
            {collections.map((collection) => {
              const isInCollection = bookId ? bookInCollectionStatus[collection.collectionId] || false : selectedCollections.has(collection.collectionId);
              const isProcessing = processingCollections.has(collection.collectionId);

              return (
                <div
                  key={collection.collectionId}
                  onClick={() => !isProcessing && handleCollectionClick(collection.collectionId)}
                  className={`collection-item ${isInCollection ? 'selected-collection' : ''} ${isProcessing ? 'processing' : ''}`}
                >
                  <CardElement
                    title={collection.title}
                    description={`${collection.booksCount} книг • ${getConfidentialityText(collection.confidentiality)}`}
                    imageUrl={getImageUrlForCollection(collection)}
                    button={!!bookId}
                    addButtonLabel="Добавить в коллекцию"
                    removeButtonLabel="Убрать из коллекции"
                    onButtonClick={() => handleCollectionClick(collection.collectionId)}
                    starsCount={collection.averageRating ? collection.averageRating / 2 : undefined}
                    infoDecoration={isInCollection ? 'В коллекции' : undefined}
                    isInCollection={isInCollection}
                    starsSize="small"
                  />
                  {isProcessing && (
                    <div className="processing-overlay">
                      <div className="processing-spinner"></div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>

          {!bookId && (
            <div className="modal-actions">
              <PrimaryButton
                label={`Добавить (${selectedCollections.size})`}
                onClick={handleConfirm}
                type="button"
                disabled={selectedCollections.size === 0}
              />
            </div>
          )}

          {bookId && (
            <div className="collection-status">
              <div className="selected-count">
                Книга добавлена в {selectedCollections.size} {selectedCollections.size === 1 ? 'коллекцию' : selectedCollections.size > 1 && selectedCollections.size < 5 ? 'коллекции' : 'коллекций'}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default CollectionListModal;