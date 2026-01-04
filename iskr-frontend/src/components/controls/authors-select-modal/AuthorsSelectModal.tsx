import { useState, useEffect } from 'react';
import PrimaryButton from '../primary-button/PrimaryButton';
import SecondaryButton from '../secondary-button/SecondaryButton';
import Input from '../input/Input';
import './AuthorsSelectModal.scss';
import searchAPI from '../../../api/searchService';

interface Author {
  authorId: number;
  name: string;
  realName?: string;
}

interface AuthorsSelectModalProps {
  onSelect: (authors: Author[]) => void;
  onClose: () => void;
  goBack: () => void;
  initialSelected?: Author[];
}

function AuthorsSelectModal({ onSelect, onClose, goBack, initialSelected = [] }: AuthorsSelectModalProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<any[]>([]);
  const [selectedAuthors, setSelectedAuthors] = useState<Author[]>(initialSelected);
  const [searchLoading, setSearchLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }

    const searchAuthors = async () => {
      setSearchLoading(true);
      try {
        const authors = await searchAPI.searchAuthors(searchQuery.trim());
        setSearchResults(authors);
      } catch (err: any) {
        console.error('Error searching authors:', err);
        setSearchResults([]);
      } finally {
        setSearchLoading(false);
      }
    };

    const timeoutId = setTimeout(searchAuthors, 300);
    return () => clearTimeout(timeoutId);
  }, [searchQuery]);

  const handleAddAuthor = (author: any) => {
    if (!selectedAuthors.some(a => a.authorId === author.id)) {
      setSelectedAuthors([
        ...selectedAuthors,
        {
          authorId: author.id,
          name: author.name,
          realName: author.realName
        }
      ]);
    }
  };

  const handleRemoveAuthor = (authorId: number) => {
    setSelectedAuthors(selectedAuthors.filter(a => a.authorId !== authorId));
  };

  const handleSave = () => {
    if (selectedAuthors.length === 0) {
      setError('Выберите хотя бы одного автора');
      return;
    }
    onSelect(selectedAuthors);
  };

  return (
    <div className="authors-select-modal">
      <div className="modal-header">
        <button className="back-button" onClick={goBack}>
          ← Назад
        </button>
        <h2 className="modal-title">Выбор авторов</h2>
      </div>

      <div className="modal-content">
        <div className="selected-authors-section">
          <h3 className="section-title">Выбранные авторы</h3>
          {selectedAuthors.length > 0 ? (
            <div className="authors-list">
              {selectedAuthors.map(author => (
                <div key={author.authorId} className="author-item">
                  <div className="author-info">
                    <span className="author-name">{author.name}</span>
                    {author.realName && (
                      <span className="author-real-name">({author.realName})</span>
                    )}
                  </div>
                  <button
                    className="remove-author-btn"
                    onClick={() => handleRemoveAuthor(author.authorId)}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          ) : (
            <p className="no-authors-message">Авторы не выбраны</p>
          )}
        </div>

        <div className="search-section">
          <h3 className="section-title">Поиск авторов</h3>
          <Input
            type="text"
            placeholder="Начните вводить имя автора..."
            value={searchQuery}
            onChange={setSearchQuery}
          />
          
          {searchLoading ? (
            <div className="search-loading">Поиск...</div>
          ) : searchResults.length > 0 ? (
            <div className="search-results">
              {searchResults
                .filter(author => !selectedAuthors.some(a => a.authorId === author.id))
                .map(author => (
                  <div
                    key={author.id}
                    className="search-result-item"
                    onClick={() => handleAddAuthor(author)}
                  >
                    <div className="author-info">
                      <span className="author-name">{author.name}</span>
                      {author.realName && (
                        <span className="author-real-name">({author.realName})</span>
                      )}
                    </div>
                    <div className="add-author-btn">+</div>
                  </div>
                ))}
            </div>
          ) : searchQuery.trim() ? (
            <p className="no-results-message">Авторы не найдены</p>
          ) : null}
        </div>

        {error && (
          <div className="error-message">
            <span className="error-icon">⚠</span>
            <span>{error}</span>
          </div>
        )}

        <div className="modal-actions">
          <SecondaryButton
            label="Отмена"
            onClick={onClose}
          />
          <PrimaryButton
            label="Выбрать"
            onClick={handleSave}
          />
        </div>
      </div>
    </div>
  );
}

export default AuthorsSelectModal;