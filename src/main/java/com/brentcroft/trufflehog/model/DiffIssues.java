package com.brentcroft.trufflehog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RequiredArgsConstructor
@Getter
public class DiffIssues
{
    private final DiffEntry diffEntry;
    private final String diffText;
    private final Set< Issue > issues;

    public String toString()
    {
        return format(
                "path=[%s] parent=[%s] issues=[%s]%n    %s",
                diffEntry.getNewPath(),
                diffEntry.getNewId().name(),
                issues.size(),
                issues
                        .stream()
                        .map( Object::toString )
                        .collect( Collectors.joining( "\n    " ) )
        );
    }
}
