package com.brentcroft.trufflehog.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.diff.DiffEntry;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@RequiredArgsConstructor
@Getter
public class DiffIssues
{
    private final DiffEntry diffEntry;
    private final List< Issue > issues;

    public String toString ()
    {
        return format (
                "path=[%s] parent=[%s]%n    %s",
                diffEntry.getOldPath (),
                diffEntry.getOldId ().name (),
                issues
                        .stream ()
                        .map ( Object::toString )
                        .collect ( Collectors.joining ( "\n    " ) )
        );
    }
}
