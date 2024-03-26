package com.yeoboya.lunch.api.v1.board.repository;


import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.yeoboya.lunch.api.v1.board.domain.Board;
import com.yeoboya.lunch.api.v1.board.request.BoardSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.yeoboya.lunch.api.v1.board.domain.QBoard.board;
import static com.yeoboya.lunch.api.v1.board.domain.QBoardHashTag.boardHashTag;
import static com.yeoboya.lunch.api.v1.board.domain.QHashTag.hashTag;
import static com.yeoboya.lunch.api.v1.board.domain.QReply.reply;
import static com.yeoboya.lunch.api.v1.file.domain.QFile.file;
import static com.yeoboya.lunch.api.v1.member.domain.QMember.member;

@Repository
public class BoardRepositoryCustomImpl implements BoardRepositoryCustom{

    private final JPAQueryFactory query;

    public BoardRepositoryCustomImpl(JPAQueryFactory query) {
        this.query = query;
    }

    /**
     * Retrieves a page of boards based on the given search criteria and pageable parameters.
     *
     * @param boardSearch - the search criteria to apply
     * @param pageable - the pageable parameters for pagination
     * @return a page of boards
     */
    @Override
    public Page<Board> boardList(BoardSearch boardSearch, Pageable pageable) {
        // 메인 콘텐츠 쿼리: 콘텐츠 조회
        List<Board> content = query.selectFrom(board)
                .leftJoin(board.boardHashTags, boardHashTag)
                .leftJoin(boardHashTag.hashTag, hashTag)
                .leftJoin(board.member, member)
                .leftJoin(board.files, file)
                .leftJoin(board.replies, reply)
                .limit(pageable.getPageSize())
                .offset(pageable.getOffset())
                .orderBy(board.id.desc())
                .distinct()
                .fetch();

        // 카운트 쿼리: 총 개수 계산
        // 여기서는 총 개수를 계산하는 쿼리를 최적화하여, 필요한 경우에만 실행하도록 할 수 있습니다.
        JPAQuery<Long> countQuery = query
                .select(board.countDistinct())
                .from(board)
                .leftJoin(board.boardHashTags, boardHashTag)
                .leftJoin(boardHashTag.hashTag, hashTag)
                .leftJoin(board.member, member)
                .leftJoin(board.files, file)
                .leftJoin(board.replies, reply);

        // PageableExecutionUtils.getPage 메소드를 사용하여 필요할 때만 카운트 쿼리를 실행합니다.
        // 이 방법은 콘텐츠 리스트가 페이지 사이즈에 도달하지 않았거나, 마지막 페이지인 경우 총 개수 쿼리를 실행하지 않도록 합니다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
