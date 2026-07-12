DELETE FROM vocabulary_word
WHERE id IN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (PARTITION BY word ORDER BY id) AS row_number
        FROM vocabulary_word
    ) duplicate_words
    WHERE row_number > 1
);

ALTER TABLE vocabulary_word
    ADD CONSTRAINT uk_vocabulary_word_word UNIQUE (word);
