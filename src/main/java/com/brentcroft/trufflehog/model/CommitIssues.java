package com.brentcroft.trufflehog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RequiredArgsConstructor
@ToString
@Getter
public class CommitIssues
{
    private final RevCommit commit;
    private final List< DiffIssues > diffIssues = new ArrayList<>();


    public boolean hasIssues()
    {
        return ! diffIssues.isEmpty();
    }

    public String toString()
    {
        PersonIdent person = commit.getAuthorIdent();
        return format(
                "commit%n  sha=[%s]%n  date=[ %s ]%n  author=[%s]%n  %s",
                commit.getId().getName(),
                person.getWhen(),
                person.getName(),
                diffIssues
                        .stream()
                        .map( Object::toString )
                        .collect( Collectors.joining( "\n  " ) )
        );
    }
}
