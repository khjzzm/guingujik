package com.yeoboya.lunch.api.v1.board.repository;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.yeoboya.lunch.api.v1.board.domain.Board;
import com.yeoboya.lunch.api.v1.board.request.BoardSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.yeoboya.lunch.api.v1.board.domain.QBoard.board;
import static com.yeoboya.lunch.api.v1.board.domain.QBoardHashTag.boardHashTag;
import static com.yeoboya.lunch.api.v1.board.domain.QHashTag.hashTag;
import static com.yeoboya.lunch.api.v1.file.domain.QFile.file;
import static com.yeoboya.lunch.api.v1.member.domain.QMember.member;

@Repository
public class BoardRepositoryCustomImpl implements BoardRepositoryCustom{

    private final JPAQueryFactory query;

    public BoardRepositoryCustomImpl(JPAQueryFactory query) {
        this.query = query;
    }

    @Override
    public Page<Board> boardList(BoardSearch boardSearch, Pageable pageable) {
        List<Board> content = query.selectFrom(board)
                .leftJoin(board.boardHashTags, boardHashTag)
                .leftJoin(boardHashTag.hashTag, hashTag)
                .leftJoin(board.member, member)
                .leftJoin(board.files, file)
                .distinct()
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(board.id.desc())
                .fetch();

        Long totalCount = query
                .select(board.count())
                .from(board)
                .fetchOne();

        return new PageImpl<>(content, pageable, totalCount);
    }
}
