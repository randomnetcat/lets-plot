#
# Copyright (c) 2019. JetBrains s.r.o.
# Use of this source code is governed by the MIT license that can be found in the LICENSE file.
#
from .core import FeatureSpec, FeatureSpecArray
from .scale import _scale

#
# Plot title
# Scale names: axis labels / legend titles
#
__all__ = ['ggtitle',
           'labs',
           'xlab', 'ylab']


def ggtitle(label):
    """
    Add title to the plot.

    Parameters
    ----------
    label : str
        The text for the plot title.

    Returns
    --------
    `FeatureSpec`
        Plot title specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            ggtitle('New Plot Title')

    """
    return labs(title=label)


def xlab(label):
    """
    Add label to the x axis.

    Parameters
    ----------
    label : str
        The text for the x axis label.

    Returns
    -------
    `FeatureSpec`
        Axis label specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            xlab('x axis label')

    """
    return labs(x=label)


def ylab(label):
    """
    Add label to the y axis.

    Parameters
    ----------
    label : str
        The text for the y axis label.

    Returns
    -------
    `FeatureSpec`
        Axis label specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            ylab('y axis label')

    """
    return labs(y=label)


def labs(**kwargs):
    """
    Change plot title, axis labels and legend titles.

    Parameters
    ----------
    kwargs : dict
        A list of new name-value pairs where name should be an aesthetic,
        e.g. title='Plot title' or aesthetic='Scale label'.

    Returns
    -------
    `FeatureSpec` or `FeatureSpecArray`
        Axis label specification.

    Examples
    --------
    .. jupyter-execute::
        :linenos:
        :emphasize-lines: 5-6
        
        from lets_plot import *
        LetsPlot.setup_html()
        data = {'x': list(range(10)), 'y': list(range(10))}
        ggplot(data, aes('x', 'y')) + geom_point(aes(size='y')) + \\
            labs(title='New plot title', x='New x axis label', \\
                 y='New y axis label', size='New legend title')

    """
    specs = []
    for k, v in kwargs.items():
        if k == 'title':
            specs.append(FeatureSpec('ggtitle', name=None, text=v))
        else:
            specs.append(_scale(aesthetic=k, name=v))

    if len(specs) == 1:
        return specs[0]
    return FeatureSpecArray(*specs)
